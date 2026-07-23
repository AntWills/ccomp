package com.ccomp.br.domain.events.application;


import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.util.ActivityMapper;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class ActivitiesServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final EventActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    public ActivitiesServices(EventRepository eventRepository, EventEditorRepository editorRepository, EventActivityRepository activityRepository, ActivityMapper activityMapper) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
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
