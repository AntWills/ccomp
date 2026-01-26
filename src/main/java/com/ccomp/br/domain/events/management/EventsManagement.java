package com.ccomp.br.domain.events.management;

import com.ccomp.br.domain.events.persistence.EnrollmentsModel;
import com.ccomp.br.domain.events.persistence.EnrollmentsModelRepository;
import com.ccomp.br.domain.events.persistence.EventsModelRepository;
import com.ccomp.br.shared.dto.EventResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EventsManagement {
    private final EventsModelRepository eventsModelRepository;
    private final EnrollmentsModelRepository enrollmentsModelRepository;

    public EventsManagement(EventsModelRepository eventsModelRepository, EnrollmentsModelRepository enrollmentsModelRepository) {
        this.eventsModelRepository = eventsModelRepository;
        this.enrollmentsModelRepository = enrollmentsModelRepository;
    }

    public List<EventResponse> findAllByOwnerId(UUID ownerId){
        return eventsModelRepository.findAllByOwnerId(ownerId)
                .stream().map(eventsModel -> new EventResponse(eventsModel.getId(), eventsModel.getName(), eventsModel.getStart(), eventsModel.getEnd(), eventsModel.getOwnerId()))
                .toList();
    }

    public List<EventResponse> findAllSubscriptions(UUID participantId) {
        return enrollmentsModelRepository.findAllByUserIdWithEvent(participantId)
                .stream().map(enrollmentsModel -> new EventResponse(
                        enrollmentsModel.getEventsModel().getId(),
                        enrollmentsModel.getEventsModel().getName(),
                        enrollmentsModel.getEventsModel().getStart(),
                        enrollmentsModel.getEventsModel().getEnd(),
                        enrollmentsModel.getEventsModel().getOwnerId()
                )).toList();
    }
}
