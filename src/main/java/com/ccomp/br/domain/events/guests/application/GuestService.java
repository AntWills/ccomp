package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
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
    private final EventCache eventCache;
    private final ActivityCache activityCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventGuestDslRepository eventGuestDslRepository;
    private final ActivityGuestDslRepository activityGuestDslRepository;

    public GuestService(EventGuestRepository eventGuestRepository,
                        EventCache eventCache,
                        ActivityCache activityCache,
                        EventAccessPolicy eventAccessPolicy,
                        EventGuestDslRepository eventGuestDslRepository,
                        ActivityGuestDslRepository activityGuestDslRepository) {
        this.eventGuestRepository = eventGuestRepository;
        this.eventCache = eventCache;
        this.activityCache = activityCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventGuestDslRepository = eventGuestDslRepository;
        this.activityGuestDslRepository = activityGuestDslRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventGuestDTO> searchEventGuests(Long eventId, UUID requesterId, String cursorRaw, int pageSize) {
        int limit = Math.min(pageSize, 50);
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean isManager = eventAccessPolicy.canEdit(event, requesterId);

        if (!eventAccessPolicy.canView(event, requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para visualizar os convidados deste evento.");
        }

        GuestCursor decodedCursor = CursorUtils.decode(cursorRaw, GuestCursor.class);

        List<EventGuestDTO> results = eventGuestDslRepository.findGuestsWithCursor(eventId, isManager, decodedCursor, limit);
        return CursorUtils.buildPage(
                results, limit, guest -> new GuestCursor(guest.id(), guest.createdAt())
        );
    }

    @Transactional(readOnly = true)
    public CursorPage<ActivityGuestDTO> searchActivityGuests(Long activityId, UUID requesterId, String cursorRaw, int pageSize) {
        int limit = Math.min(pageSize, 50);
        ActivityDTO activity = activityCache.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        EventDTO event = eventCache.findById(activity.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        boolean isManager = eventAccessPolicy.canEdit(event, requesterId);

        if (!eventAccessPolicy.canView(event, requesterId))
            throw new AccessDeniedException("Você não tem permissão para visualizar os convidados desta atividade.");

        GuestCursor decodedCursor = CursorUtils.decode(cursorRaw, GuestCursor.class);

        List<ActivityGuestDTO> results = activityGuestDslRepository.findActivityGuestsWithCursor(activityId, isManager, decodedCursor, limit);
        return CursorUtils.buildPage(
                results, limit, guest -> new GuestCursor(guest.id(), guest.createdAt())
        );
    }

    @Transactional
    public void updateEventGuest(Long eventId, Long guestId, UUID requesterId, UpdateGuestDTO dto) {
        EventGuest guest = eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));

        EventDTO event = eventCache.findById(guest.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        boolean isManager = eventAccessPolicy.canEdit(event, requesterId);
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
}
