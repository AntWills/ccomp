package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.EventsModel;
import com.ccomp.br.domain.events.persistence.EventsModelRepository;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventsApplication {
    private final EventsModelRepository eventsModelRepository;
    private final UserManagement userManagement;

    public EventsApplication(EventsModelRepository eventsModelRepository, UserManagement userManagement) {
        this.eventsModelRepository = eventsModelRepository;
        this.userManagement = userManagement;
    }

    public EventResponse create(UUID id, CreateEventRequestDTO dto){
        if(userManagement.findById(id).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + id);
        
        var eventsModel = new EventsModel(dto.name(), id);

        dto.getStartDate().ifPresent(eventsModel::setStart);
        dto.getEndDate().ifPresent(eventsModel::setEnd);
        
        var savedEvent = eventsModelRepository.save(new EventsModel(dto.name(), id));

        return new EventResponse(savedEvent.getId(), savedEvent.getName(), savedEvent.getStart(), savedEvent.getEnd(), savedEvent.getOwnerId());
    }

    public Optional<EventResponse> getById(Long eventId, UUID ownerId){
        return eventsModelRepository.findById(eventId)
                .filter(eventsModel -> eventsModel.getOwnerId().equals(ownerId))
                .map(eventsModel ->
                        new EventResponse(
                                eventsModel.getId(),
                                eventsModel.getName(),
                                eventsModel.getStart(),
                                eventsModel.getEnd(),
                                eventsModel.getOwnerId()
                        ));
    }
}
