package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventsApplication {
    private final EventRepository eventRepository;
    private final UserManagement userManagement;

    public EventsApplication(EventRepository eventRepository, UserManagement userManagement) {
        this.eventRepository = eventRepository;
        this.userManagement = userManagement;
    }

    public EventResponse create(UUID id, CreateEventRequestDTO dto){
        if(userManagement.findById(id).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + id);
        
        var eventsModel = new Event(dto.name(), id);

        dto.getStartDate().ifPresent(eventsModel::setStart);
        dto.getEndDate().ifPresent(eventsModel::setEnd);
        
        var savedEvent = eventRepository.save(new Event(dto.name(), id));

        return new EventResponse(savedEvent.getId(), savedEvent.getName(), savedEvent.getStart(), savedEvent.getEnd(), savedEvent.getOwnerId());
    }

    public Optional<EventResponse> getById(Long eventId, UUID ownerId){
        return eventRepository.findById(eventId)
                .filter(event -> event.getOwnerId().equals(ownerId))
                .map(event ->
                        new EventResponse(
                                event.getId(),
                                event.getName(),
                                event.getStart(),
                                event.getEnd(),
                                event.getOwnerId()
                        ));
    }
}
