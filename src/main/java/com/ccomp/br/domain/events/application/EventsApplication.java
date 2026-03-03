package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventsApplication {
    private final EventRepository eventRepository;
    private final UserManagement userManagement;
    private final EventMapper mapper;

    public EventsApplication(EventRepository eventRepository, UserManagement userManagement, EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.userManagement = userManagement;
        this.mapper = mapper;
    }

    public EventResponse create(UUID id, CreateEventRequestDTO dto){
        if(userManagement.findById(id).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + id);
        
        var eventsModel = new Event(dto.name(), id);

        dto.getStartDate().ifPresent(eventsModel::setStart);
        dto.getEndDate().ifPresent(eventsModel::setEnd);
        
        var savedEvent = eventRepository.save(new Event(dto.name(), id));

        return mapper.eventToEventResponse(savedEvent);
    }

    public Optional<EventResponse> getById(Long eventId, UUID userId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    boolean allowed =
                            event.isOpen()
                                    || Optional.ofNullable(userId).filter(event::isOwner).isPresent();

                    if (allowed) return mapper.eventToEventResponse(event);

                    throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
                });
    }
}
