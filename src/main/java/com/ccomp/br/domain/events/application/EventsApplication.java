package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.domain.events.util.ActivityMapper;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventsApplication {
    private final EventRepository eventRepository;
    private final EventActivityRepository activityRepository;
    private final UserManagement userManagement;
    private final EventMapper eventMapper;
    private final ActivityMapper activityMapper;

    public EventsApplication(EventRepository eventRepository, EventActivityRepository activityRepository, UserManagement userManagement, EventMapper eventMapper, ActivityMapper activityMapper) {
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.userManagement = userManagement;
        this.eventMapper = eventMapper;
        this.activityMapper = activityMapper;
    }

    public EventResponse create(UUID id, CreateEventRequestDTO dto){
        if(userManagement.findById(id).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + id);
        
        var eventsModel = new Event(dto.name(), id);

        dto.getStartDate().ifPresent(eventsModel::setStart);
        dto.getEndDate().ifPresent(eventsModel::setEnd);
        
        var savedEvent = eventRepository.save(new Event(dto.name(), id));

        return eventMapper.eventToEventResponse(savedEvent);
    }

    public Optional<EventResponse> getById(Long eventId, UUID userId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    boolean allowed =
                            event.isOpen()
                                    || Optional.ofNullable(userId).filter(event::isOwner).isPresent();

                    if (allowed) return eventMapper.eventToEventResponse(event);

                    throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
                });
    }

    public ActivityDTO createActivity(UUID userId, CreateActivityRequest request){
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(userId) && !activityRepository.existsByUserIdAndEvent(userId, event))
            throw new AccessDeniedException("O usuario não tem acesso a este evento.");

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(request.title())
                .description(request.description())
                .build();

        EventActivity activitySaved = activityRepository.save(activity);

        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }
}
