package com.ccomp.br.domain.events.management;

import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.shared.dto.EventListItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EventsManagement {
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EventMapper eventMapper;

    public EventsManagement(EventRepository eventRepository, EnrollmentRepository enrollmentRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventMapper = eventMapper;
    }

    public List<EventListItem> findAllByOwnerId(UUID ownerId){
        return eventRepository.findAllByOwnerId(ownerId)
                .stream().map(eventMapper::eventToEventResponse)
                .toList();
    }

    public List<EventListItem> findAllSubscriptions(UUID participantId) {
        return enrollmentRepository.findAllByUserIdWithEvent(participantId)
                .stream().map(enrollmentsModel -> eventMapper.eventToEventResponse(enrollmentsModel.getEvent())
                ).toList();
    }
}
