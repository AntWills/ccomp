package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GuestService {

    private final EventGuestRepository eventGuestRepository;
    private final EventRepository eventRepository;
    private final EventActivityRepository activityRepository;
    private final EventEditorRepository editorRepository;
    private final EventGuestDslRepository eventGuestDslRepository;
    private final ActivityGuestDslRepository activityGuestDslRepository;

    public GuestService(EventGuestRepository eventGuestRepository, EventRepository eventRepository, EventActivityRepository activityRepository, EventEditorRepository editorRepository, EventGuestDslRepository eventGuestDslRepository, ActivityGuestDslRepository activityGuestDslRepository) {
        this.eventGuestRepository = eventGuestRepository;
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.editorRepository = editorRepository;
        this.eventGuestDslRepository = eventGuestDslRepository;
        this.activityGuestDslRepository = activityGuestDslRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventGuest> searchEventGuests(Long eventId, UUID requesterId, String cursorRaw, int pageSize) {
        int limit = Math.min(pageSize, 50);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean isManager = requesterId != null && canManageEvent(requesterId, event);

        if (!isManager && !event.isPublished()) {
            throw new AccessDeniedException("O evento não está publicado.");
        }

        GuestCursor decodedCursor = CursorUtils.decode(cursorRaw, GuestCursor.class);

        List<EventGuest> results = eventGuestDslRepository.findGuestsWithCursor(eventId, isManager, decodedCursor, limit);

        return CursorUtils.buildPage(results, limit, guest -> new GuestCursor(guest.getId(), guest.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public CursorPage<ActivityGuest> searchActivityGuests(Long activityId, UUID requesterId, String cursorRaw, int pageSize) {
        int limit = Math.min(pageSize, 50);
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        Event event = activity.getEvent();
        boolean isManager = requesterId != null && canManageEvent(requesterId, event);

        if (!isManager && !event.isPublished()) {
            throw new AccessDeniedException("O evento não está publicado.");
        }

        GuestCursor decodedCursor = CursorUtils.decode(cursorRaw, GuestCursor.class);

        List<ActivityGuest> results = activityGuestDslRepository.findActivityGuestsWithCursor(activityId, isManager, decodedCursor, limit);

        return CursorUtils.buildPage(results, limit, guest -> new GuestCursor(guest.getId(), guest.getCreatedAt()));
    }

    @Transactional
    public void updateEventGuest(Long eventId, Long guestId, UUID requesterId, UpdateGuestDTO dto) {
        EventGuest guest = eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));

        Event event = guest.getEvent();
        boolean isManager = canManageEvent(requesterId, event);
        boolean isTheGuestHimself = guest.getUserId().equals(requesterId);

        if (!isManager && !isTheGuestHimself) {
            throw new AccessDeniedException("Você não tem permissão para alterar os dados deste convidado.");
        }

        if (dto.status() != null) {
            guest.setStatus(dto.status());
        }
        if (dto.visibility() != null) {
            guest.setVisibility(dto.visibility());
        }

        eventGuestRepository.save(guest);
    }

    private boolean canManageEvent(UUID userId, Event event) {
        return event.isOwner(userId) || editorRepository.existsByEventIdAndUserId(event.getId(), userId);
    }
}