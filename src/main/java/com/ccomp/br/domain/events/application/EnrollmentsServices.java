package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class EnrollmentsServices {
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentsServices(EventRepository eventRepository, EnrollmentRepository enrollmentRepository) {
        this.eventRepository = eventRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public MessageResponse subscribe(UUID userId, Long eventId) {
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

            if (enrollment.isActive()) {
                throw new ConflictException("Você já está inscrito neste evento.");
            }

            // Se a inscrição estava cancelada previamente, reativa mantendo o mesmo registro no banco
            enrollment.setStatus(EnumEnrollmentState.CONFIRMED);
            enrollmentRepository.save(enrollment);
        }

        Enrollment newEnrollment = Enrollment.builder()
                .userId(userId)
                .event(event)
                .status(EnumEnrollmentState.CONFIRMED)
                .build();

        enrollmentRepository.save(newEnrollment);

        return new MessageResponse("Inscrição realizada com sucesso.");
    }

    @Transactional
    public MessageResponse unsubscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ResourceNotFoundException("Você não possui uma inscrição neste evento."));

        if (enrollment.getStatus() == EnumEnrollmentState.CANCELED)
            throw new ConflictException("Sua inscrição neste evento já se encontra cancelada.");

        enrollment.cancel();
        enrollmentRepository.save(enrollment);

        return new MessageResponse("Inscrição removida com sucesso.");
    }
}
