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

@Service
public class EnrollmentsServices {
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentDslRepository enrollmentDslRepository;
    private final EventAccessPolicy eventAccessPolicy;

    public EnrollmentsServices(EventRepository eventRepository, EventCache eventCache,
                               EnrollmentRepository enrollmentRepository,
                               EnrollmentDslRepository enrollmentDslRepository,
                               EventAccessPolicy eventAccessPolicy) {
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentDslRepository = enrollmentDslRepository;
        this.eventAccessPolicy = eventAccessPolicy;
    }

    @Transactional(readOnly = true)
    public CursorPage<EnrollmentListItem> searchEnrollments(Long eventId, UUID userId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);

        EventDTO event = eventCache.findById(eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        EnrollmentsCursor cursorDecoded = CursorUtils.decode(cursor, EnrollmentsCursor.class);
        List<EnrollmentListItem> results = enrollmentDslRepository
                .findAllWithCursor(eventId, cursorDecoded, finalPageSize + 1);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                i -> new EnrollmentsCursor(i.createdAt(), i.id())
        );
    }

    @Transactional
    public Enrollment subscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        EnumEnrollmentStatus enrollmentStatus = event.getEnrollmentStatus();
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

            if (enrollment.isActive())
                return enrollment;


            // Se a inscrição estava cancelada previamente, reativa mantendo o mesmo registro no banco
            enrollment.confirm();
            enrollmentRepository.save(enrollment);
            return enrollment;
        }

        Enrollment newEnrollment = Enrollment.builder()
                .userId(userId)
                .event(event)
                .status(EnumEnrollmentState.CONFIRMED)
                .build();

        return enrollmentRepository.save(newEnrollment);
    }

    @Transactional
    public MessageResponse unsubscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ResourceNotFoundException("Você não possui uma inscrição neste evento."));

        enrollment.cancel();
        enrollmentRepository.save(enrollment);

        return new MessageResponse("Inscrição removida com sucesso.");
    }

    @Transactional
    public MessageResponse updateEnrollmentStatus(Long eventId, Long enrollmentId,
                                                   EnumEnrollmentState newStatus, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Você não tem permissão para alterar o status das inscrições deste evento.");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada."));

        if (!enrollment.getEventId().equals(eventId))
            throw new ResourceNotFoundException("Inscrição não encontrada neste evento.");

        switch (newStatus) {
            case CONFIRMED -> enrollment.confirm();
            case CHECKED_IN -> enrollment.checkIn();
            case CANCELED -> enrollment.cancel();
        }

        enrollmentRepository.save(enrollment);
        return new MessageResponse("Status da inscrição alterado para: " + newStatus.name());
    }
}
