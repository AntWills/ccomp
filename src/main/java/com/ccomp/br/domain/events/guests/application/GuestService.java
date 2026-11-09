package com.ccomp.br.domain.events.guests.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.dto.CreateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestCache;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestService {

    private final EventGuestRepository eventGuestRepository;
    private final EventCache eventCache;
    private final ActivityCache activityCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventGuestCache eventGuestCache;
    private final ActivityGuestDslRepository activityGuestDslRepository;
    private final EventRepository eventRepository;
    private final EventActivityRepository activityRepository;
    private final ActivityGuestRepository activityGuestRepository;
    private final UserManagement userManagement;

    public GuestService(
            EventGuestRepository eventGuestRepository,
            EventCache eventCache,
            ActivityCache activityCache,
            EventAccessPolicy eventAccessPolicy,
            EventGuestCache eventGuestCache,
            ActivityGuestDslRepository activityGuestDslRepository,
            EventRepository eventRepository,
            EventActivityRepository activityRepository,
            ActivityGuestRepository activityGuestRepository,
            UserManagement userManagement
    ) {
        this.eventGuestRepository = eventGuestRepository;
        this.eventCache = eventCache;
        this.activityCache = activityCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventGuestCache = eventGuestCache;
        this.activityGuestDslRepository = activityGuestDslRepository;
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.activityGuestRepository = activityGuestRepository;
        this.userManagement = userManagement;
    }

    @Transactional
    public void addGuestToActivity(
            Long eventId,
            Long guestId,
            Long activityId,
            EnumActivityGuestRole role,
            UUID requesterId
    ) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar convidados deste evento.");
        }

        EventGuest guest = eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));

        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if (!activity.getEventId().equals(eventId)) {
            throw new ResourceNotFoundException("A atividade não pertence a este evento.");
        }

        EnumActivityGuestRole selectedRole = role == null
                ? EnumActivityGuestRole.PRESENTER
                : role;
        ActivityGuest activityGuest = activityGuestRepository
                .findByActivityIdAndEventGuestId(activityId, guestId)
                .orElse(null);

        if (activityGuest == null) {
            activityGuestRepository.save(
                    ActivityGuest.builder()
                            .activity(activity)
                            .eventGuest(guest)
                            .role(selectedRole)
                            .build()
            );
            return;
        }

        if (activityGuest.getRole() != selectedRole) {
            activityGuest.setRole(selectedRole);
            activityGuestRepository.save(activityGuest);
        }
    }

    @Transactional
    public void linkAccount(Long eventId, Long guestId, UUID requesterId, String guestEmail, Long activityId) {
        EventGuest guest = eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));

        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Você não tem permissão para gerenciar convidados deste evento.");

        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if (!activity.getEventId().equals(eventId))
            throw new ResourceNotFoundException("A atividade não pertence a este evento.");

        var guestUser = userManagement.findByEmailAddress(new EmailAddress(guestEmail))
                .filter(user -> user.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Não há uma conta ativa com este e-mail."));
        UUID guestUserId = guestUser.id();

        guest.setUserId(guestUserId);
        eventGuestRepository.save(guest);
        eventGuestCache.evict(guest);

        if (!activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, guestUserId))
            activityGuestRepository.save(
                    ActivityGuest.builder()
                            .activity(activity)
                            .eventGuest(guest)
                            .build()
            );
    }

    @Transactional
    public EventGuest createGuest(Long eventId, UUID requesterId, CreateGuestDTO request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        EventDTO eventDto = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(eventDto, requesterId))
            throw new AccessDeniedException("Você não tem permissão para gerenciar convidados deste evento.");

        EventGuest guest = eventGuestRepository.save(
                EventGuest.builder()
                        .event(event)
                        .name(request.name())
                        .description(request.description())
                        .userId(resolveActiveAccount(request.accountEmail()))
                        .build()
        );
        eventGuestCache.evict(guest);

        return guest;
    }

    private UUID resolveActiveAccount(String email) {
        if (email == null || email.isBlank()) return null;
        return userManagement.findByEmailAddress(new EmailAddress(email))
                .filter(user -> user.isActive())
                .map(user -> user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Não há uma conta ativa com este e-mail."));
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

        List<EventGuestDTO> results = eventGuestCache.findGuestsWithCursor(eventId, isManager, decodedCursor, limit);
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
        boolean isTheGuestHimself = requesterId != null && Objects.equals(guest.getUserId(), requesterId);

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
        eventGuestCache.evict(guest);
    }

    @Transactional
    public void updateGuestProfile(
            Long eventId,
            Long guestId,
            UUID requesterId,
            UpdateGuestProfileDTO request
    ) {
        EventGuest guest = eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));

        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean isTheGuestHimself = requesterId != null && Objects.equals(guest.getUserId(), requesterId);
        if (!eventAccessPolicy.canEdit(event, requesterId) && !isTheGuestHimself) {
            throw new AccessDeniedException("Você não tem permissão para editar este convidado.");
        }

        guest.setName(request.name());
        guest.setDescription(request.description());
        eventGuestRepository.save(guest);
        eventGuestCache.evict(guest);
    }

    @Transactional(readOnly = true)
    public boolean canEditActivity(Long activityId, UUID requesterId) {
        return requesterId != null
                && userManagement.isAccountActive(requesterId)
                && activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, requesterId);
    }
}
