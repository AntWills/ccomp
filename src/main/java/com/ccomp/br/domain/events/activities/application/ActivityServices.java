package com.ccomp.br.domain.events.activities.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.dto.*;
import com.ccomp.br.domain.events.shared.dto.CapacityUpdateRequest;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.instructor.application.InstructorService;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ActivityServices {
    private final EventRepository eventRepository;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventCache eventCache;
    private final EventMapper eventMapper;
    private final EventActivityRepository activityRepository;
    private final EventActivityDslRepository activityDslRepository;
    private final ActivityMapper activityMapper;
    private final ActivityCache activityCache;
    private final GuestService guestService;
    private final InstructorService instructorService;

    public ActivityServices(EventRepository eventRepository,
                            EventAccessPolicy eventAccessPolicy,
                            EventCache eventCache, EventMapper eventMapper,
                            EventActivityRepository activityRepository,
                            EventActivityDslRepository activityDslRepository,
                            ActivityMapper activityMapper,
                            ActivityCache activityCache,
                            GuestService guestService,
                            InstructorService instructorService) {
        this.eventRepository = eventRepository;
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventCache = eventCache;
        this.eventMapper = eventMapper;
        this.activityRepository = activityRepository;
        this.activityDslRepository = activityDslRepository;
        this.activityMapper = activityMapper;
        this.activityCache = activityCache;
        this.guestService = guestService;
        this.instructorService = instructorService;
    }

    @Transactional(readOnly = true)
    public CursorPage<ActivityDTO> searchByCursor(Long eventId, String cursor, UUID userId) {
        int pageSize = 50;
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        EventActivityCursor cursorDecoded = CursorUtils.decode(cursor, EventActivityCursor.class);
        List<ActivityDTO> results = activityCache
                .findAllByEventIdWithCursor(eventId, cursorDecoded, pageSize + 1);

        return CursorUtils.buildPage(
                results,
                pageSize,
                e -> new EventActivityCursor(e.displayOrder(), e.id()));
    }

    @Transactional
    public ActivityDTO createActivity(UUID userId, Long eventId, CreateActivityDTO request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event), userId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(request.title())
                .description(request.description())
                .registrationPolicy(EnumActivityRegistrationPolicy.EVENT_REGISTRANTS_ONLY)
                .type(EnumActivityType.OTHER)
                .maxCapacity(request.maxCapacity())
                .createdAt(LocalDateTime.now())
                .build();

        EventActivity activitySaved = activityRepository.save(activity);
        activityCache.evictEventLists();

        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }

    @Transactional
    public ActivityDTO updateActivity(UUID userId, Long activityId, UpdateActivityDTO request) {
        EventActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        EventDTO event = eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não existe."));

        boolean canEditAsManager = eventAccessPolicy.canEdit(event, userId);
        boolean canEditAsInstructor = !canEditAsManager && instructorService.canEditActivity(activityId, userId);
        boolean canEditAsGuest = !canEditAsManager && !canEditAsInstructor
                && guestService.canEditActivity(activityId, userId);
        if (!canEditAsManager && !canEditAsInstructor && !canEditAsGuest)
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");
        if (!canEditAsManager && request.maxCapacity() != null)
            throw new AccessDeniedException("Somente a equipe do evento pode alterar a capacidade da atividade.");

        activityMapper.updateEventActivityFromRequest(request, activity);
        
        EventActivity activitySaved = activityRepository.save(activity);
        activityCache.evict(activitySaved);
        
        return activityMapper.eventActivityToActivityDTO(activitySaved);
    }

    @Transactional
    public ActivityDTO updateCapacity(UUID userId, Long activityId, CapacityUpdateRequest request) {
        EventActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));
        EventDTO event = eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não existe."));
        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Somente a equipe do evento pode alterar a capacidade da atividade.");
        activity.setMaxCapacity(request.maxCapacity());
        EventActivity saved = activityRepository.save(activity);
        activityCache.evict(saved);
        return activityMapper.eventActivityToActivityDTO(saved);
    }

    @Transactional
    public void deleteActivity(UUID userId, Long activityId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        EventDTO event = eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não existe."));

        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        activityRepository.deleteById(activityId);
        activityCache.evict(activity);
    }
}
