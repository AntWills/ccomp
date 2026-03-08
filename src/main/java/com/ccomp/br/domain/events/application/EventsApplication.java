package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.util.ActivityMapper;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventsApplication {
    private final EventRepository eventRepository;
    private final EventActivityRepository activityRepository;
    private final EventEditorRepository editorRepository;
    private final UserManagement userManagement;
    private final EventMapper eventMapper;
    private final ActivityMapper activityMapper;

    public EventsApplication(EventRepository eventRepository, EventActivityRepository activityRepository, EventEditorRepository editorRepository, UserManagement userManagement, EventMapper eventMapper, ActivityMapper activityMapper) {
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.editorRepository = editorRepository;
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

    @Transactional
    public MessageResponse addEditor(Long eventId, UUID ownerId, UUID userId){
//        log.info("addEditor chamado | eventId={}, ownerId={}, userId={}",
//                eventId, ownerId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(ownerId)) throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        if(editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            return new MessageResponse("O usuario já é editor deste evento");

        if(!userManagement.userExists(userId))
            throw new UserNotFaundException("O usuario não existe.");

        editorRepository.save(EventEditor.builder()
                        .event(event)
                        .userId(userId)
                        .assignedAt(LocalDateTime.now())
                        .build());

        return new MessageResponse("Usuario adicionar como editor.");
    }

    @Transactional
    public MessageResponse removeEditor(Long eventId, UUID ownerId, UUID userId){
//        log.info("removerEditor chamado | eventId={}, ownerId={}, userId={}",
//                eventId, ownerId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(ownerId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        if(!editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            return new MessageResponse("O usuário não é editor deste evento.");

        editorRepository.deleteByEventIdAndUserId(event.getId(), userId);

        return new MessageResponse("Usuário removido como editor.");
    }

    @Transactional
    public ActivityDTO createActivity(UUID userId, Long eventId, CreateActivityRequest request){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(userId) && !editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(request.title())
                .description(request.description())
                .build();

        EventActivity activitySaved = activityRepository.save(activity);

        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }

    @Transactional
    public void deleteActivity(UUID userId, Long activityId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        Event event = activity.getEvent();

        boolean allowed =
                event.isOwner(userId)
                        || editorRepository.existsByEventIdAndUserId(event.getId(), userId);

        if(!allowed)
            throw new AccessDeniedException("User is not allowed to delete this activity.");

        activityRepository.deleteById(activityId);
    }
}
