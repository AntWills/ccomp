package com.ccomp.br.domain.events.enrollments.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class EnrollmentsServices {
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentDslRepository enrollmentDslRepository;
    private final EventAccessPolicy eventAccessPolicy;
    private final Clock clock;

    public EnrollmentsServices(EventRepository eventRepository, EventCache eventCache,
                               EnrollmentRepository enrollmentRepository,
                               EnrollmentDslRepository enrollmentDslRepository,
                               EventAccessPolicy eventAccessPolicy, Clock clock) {
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentDslRepository = enrollmentDslRepository;
        this.eventAccessPolicy = eventAccessPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CursorPage<EnrollmentListItem> searchEnrollments(Long eventId, UUID userId, String cursor, int pageSize,
                                                             EnumEnrollmentState status) {
        int finalPageSize = Math.min(pageSize, 50);

        EventDTO event = eventCache.findById(eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        EnrollmentsCursor cursorDecoded = CursorUtils.decode(cursor, EnrollmentsCursor.class);
        List<EnrollmentListItem> results = enrollmentDslRepository
                .findAllWithCursor(eventId, status, cursorDecoded, finalPageSize + 1);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                i -> new EnrollmentsCursor(i.createdAt(), i.id())
        );
    }

    @Transactional
    public Enrollment subscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        EnumEnrollmentStatus enrollmentStatus = event.getEnrollmentStatus(LocalDateTime.now(clock));
        if (enrollmentStatus != EnumEnrollmentStatus.OPEN) {
            switch (enrollmentStatus) {
                case UPCOMING -> throw new DomainException("As inscrições para este evento ainda não foram abertas.");
                case PAUSED -> throw new DomainException("As inscrições para este evento estão temporariamente pausadas.");
                case SOLD_OUT -> throw new DomainException("As vagas para este evento estão esgotadas.");
                case CLOSED -> throw new DomainException("As inscrições para este evento estão encerradas.");
                default -> throw new DomainException("Não é possível se inscrever neste evento no momento.");
            }
        }

        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByUserIdAndEvent(userId, event);

        if (existingEnrollment.isPresent()) {
            Enrollment enrollment = existingEnrollment.get();

            if (enrollment.getStatus() == EnumEnrollmentState.BANNED)
                throw new DomainException("Você não pode se inscrever novamente neste evento.");

            if (enrollment.isActive())
                return enrollment;


            // Se a inscrição estava cancelada previamente, reativa mantendo o mesmo registro no banco
            enrollment.confirm();
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            event.registerActiveEnrollment();
            eventRepository.save(event);
            eventCache.evict(event);
            return savedEnrollment;
        }

        Enrollment newEnrollment = Enrollment.builder()
                .userId(userId)
                .event(event)
                .status(EnumEnrollmentState.CONFIRMED)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);
        event.registerActiveEnrollment();
        eventRepository.save(event);
        eventCache.evict(event);
        return savedEnrollment;
    }

    @Transactional
    public MessageResponse unsubscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ResourceNotFoundException("Você não possui uma inscrição neste evento."));

        boolean wasActive = enrollment.isActive();
        enrollment.cancel();
        enrollmentRepository.save(enrollment);
        if (wasActive) {
            event.releaseActiveEnrollment();
            eventRepository.save(event);
            eventCache.evict(event);
        }

        return new MessageResponse("Inscrição removida com sucesso.");
    }

    @Transactional
    public MessageResponse updateEnrollmentStatus(Long eventId, Long enrollmentId,
                                                   EnumEnrollmentState newStatus, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Você não tem permissão para alterar o status das inscrições deste evento.");

        Event eventEntity = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada."));

        if (!enrollment.getEventId().equals(eventId))
            throw new ResourceNotFoundException("Inscrição não encontrada neste evento.");

        boolean wasActive = enrollment.isActive();
        switch (newStatus) {
            case CONFIRMED -> enrollment.confirm();
            case CHECKED_IN -> enrollment.checkIn(LocalDateTime.now(clock));
            case CANCELED -> enrollment.cancel();
            case BANNED -> enrollment.ban();
        }

        enrollmentRepository.save(enrollment);
        boolean isActive = enrollment.isActive();
        if (wasActive != isActive) {
            if (isActive) eventEntity.registerActiveEnrollment();
            else eventEntity.releaseActiveEnrollment();
            eventRepository.save(eventEntity);
            eventCache.evict(eventEntity);
        }
        return new MessageResponse("Status da inscrição alterado para: " + newStatus.name());
    }
}
