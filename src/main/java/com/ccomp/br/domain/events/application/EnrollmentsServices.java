package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (enrollmentRepository.existsByUserIdAndEvent(userId, event))
            throw new ConflictException("O usuário já está inscrito neste evento.");

        enrollmentRepository.save(new Enrollment(userId, event));

        return new MessageResponse("Inscrição realizada com sucesso.");
    }

    @Transactional
    public MessageResponse unsubscribe(UUID userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!enrollmentRepository.existsByUserIdAndEvent(userId, event))
            throw new ResourceNotFoundException("O usuário não está inscrito neste evento.");

        enrollmentRepository.deleteByUserIdAndEvent(userId, event);

        return new MessageResponse("Inscrição removida com sucesso.");
    }
}
