package com.ccomp.br.domain.events.enrollments.application;

import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentBlaze;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.events.enrollments.util.EnrollmentMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserSummaryView;
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
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentBlaze enrollmentBlaze;
    private final UserManagement userManagement;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentsServices(EventRepository eventRepository, EnrollmentRepository enrollmentRepository, EnrollmentBlaze enrollmentBlaze, UserManagement userManagement, EnrollmentMapper enrollmentMapper) {
        this.eventRepository = eventRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentBlaze = enrollmentBlaze;
        this.userManagement = userManagement;
        this.enrollmentMapper = enrollmentMapper;
    }

    @Transactional(readOnly = true)
    public CursorPage<EnrollmentListItem> searchEnrollments(Long eventId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);

        EnrollmentsCursor cursorDecoded = CursorUtils.decode(cursor, EnrollmentsCursor.class);

        List<Enrollment> results = enrollmentBlaze.findByCursor(eventId, cursorDecoded, finalPageSize);

        List<UserSummaryView> userSummaryViews = userManagement.findAllSummaryByIds(results
                .stream()
                .map(Enrollment::getUserId)
                .toList());

        // Realiza o de-para de Enrollment -> EnrollmentListItem
        List<EnrollmentListItem> items = enrollmentMapper.toListItemList(results, userSummaryViews);

        return CursorUtils.buildPage(items, finalPageSize, i -> new EnrollmentsCursor(i.createdAt(), i.id()));
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
            enrollment.setStatus(EnumEnrollmentState.CONFIRMED);
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
}
