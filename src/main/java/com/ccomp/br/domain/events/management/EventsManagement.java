package com.ccomp.br.domain.events.management;

import com.ccomp.br.domain.events.persistence.EnrollmentRepository;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.shared.dto.EventResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EventsManagement {
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EventsManagement(EventRepository eventRepository, EnrollmentRepository enrollmentRepository) {
        this.eventRepository = eventRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EventResponse> findAllByOwnerId(UUID ownerId){
        return eventRepository.findAllByOwnerId(ownerId)
                .stream().map(eventsModel -> new EventResponse(eventsModel.getId(), eventsModel.getName(), eventsModel.getStart(), eventsModel.getEnd(), eventsModel.getOwnerId()))
                .toList();
    }

    public List<EventResponse> findAllSubscriptions(UUID participantId) {
        return enrollmentRepository.findAllByUserIdWithEvent(participantId)
                .stream().map(enrollmentsModel -> new EventResponse(
                        enrollmentsModel.getEvent().getId(),
                        enrollmentsModel.getEvent().getName(),
                        enrollmentsModel.getEvent().getStart(),
                        enrollmentsModel.getEvent().getEnd(),
                        enrollmentsModel.getEvent().getOwnerId()
                )).toList();
    }
}
