package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.domain.events.dto.EventActivityCursor;
import com.ccomp.br.domain.events.dto.UpdateActivityRequest;
import com.ccomp.br.domain.events.dto.EventActivityView;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityBlaze;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.util.ActivityMapper;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ActivitiesServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final EventActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final EventActivityBlaze eventActivityBlaze;

    public ActivitiesServices(EventRepository eventRepository, EventEditorRepository editorRepository,
            EventActivityRepository activityRepository, ActivityMapper activityMapper,
            EventActivityBlaze eventActivityBlaze) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
        this.eventActivityBlaze = eventActivityBlaze;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventActivityView> searchByCursor(Long eventId, String cursor, UUID userId) {
        int pageSize = 50;
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean allowed = event.isOpen()
                || event.isOwner(userId)
                || (userId != null && editorRepository.existsByEventIdAndUserId(event.getId(), userId))
                || SecurityUtils.isAdmin();

        if (!allowed)
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        EventActivityCursor cursorDecoded = CursorUtils.decode(cursor, EventActivityCursor.class).orElse(null);
        List<EventActivityView> results = eventActivityBlaze.findByCursor(eventId, cursorDecoded, pageSize + 1);

        return CursorUtils.buildPage(results, pageSize, e -> new EventActivityCursor(e.getCreatedAt(), e.getId()));
    }

    @Transactional
    public ActivityDTO createActivity(UUID userId, Long eventId, CreateActivityRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!event.isOwner(userId) && !editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(request.title())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();

        EventActivity activitySaved = activityRepository.save(activity);

        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }

    @Transactional
    public ActivityDTO updateActivity(UUID userId, Long activityId, UpdateActivityRequest request) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        Event event = activity.getEvent();

        boolean allowed = event.isOwner(userId)
                || editorRepository.existsByEventIdAndUserId(event.getId(), userId);

        if (!allowed)
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        activityMapper.updateEventActivityFromRequest(request, activity);
        
        EventActivity activitySaved = activityRepository.save(activity);
        
        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }

    @Transactional
    public void deleteActivity(UUID userId, Long activityId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        Event event = activity.getEvent();

        boolean allowed = event.isOwner(userId)
                || editorRepository.existsByEventIdAndUserId(event.getId(), userId);

        if (!allowed)
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        activityRepository.deleteById(activityId);
    }
}
