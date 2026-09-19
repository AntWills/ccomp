package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.core.enums.EnumInvitationStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.dto.EventInvitationCursor;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitation;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitationDspRepository;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitationRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GuestServices {
    private final EventGuestRepository eventGuestRepository;
    private final EventInvitationRepository eventInvitationRepository;
    private final EventInvitationDspRepository eventInvitationDspRepository;
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final UserManagement userManagement;

    public GuestServices(EventGuestRepository eventGuestRepository, EventInvitationRepository eventInvitationRepository, EventInvitationDspRepository eventInvitationDspRepository, EventRepository eventRepository, EventEditorRepository editorRepository, UserManagement userManagement) {
        this.eventGuestRepository = eventGuestRepository;
        this.eventInvitationRepository = eventInvitationRepository;
        this.eventInvitationDspRepository = eventInvitationDspRepository;
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventInvitation> searchInvitations(
            UUID userId, @Nullable EmailAddress emailAddress, Long eventId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!canManageEvent(userId, event))
            throw new AccessDeniedException("Você não tem permissão para acessar convites neste evento.");

        EventInvitationCursor cursorDecoded = CursorUtils.decode(cursor, EventInvitationCursor.class);
        List<EventInvitation> results = eventInvitationDspRepository
                .findAllWithCursor(emailAddress, eventId, cursorDecoded, finalPageSize);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                ei -> new EventInvitationCursor(
                        ei.getId(), ei.getInvitedAt()
                )
        );
    }

    public MessageResponse invite(UUID userId, Long eventId, EmailAddress emailAddress) {
        Event event = getManageableEvent(userId, eventId);
        Optional<UserDTO> userDtoOpt = userManagement.findByEmailAddress(emailAddress);

        if (userDtoOpt.isPresent()) {
            UserDTO user = userDtoOpt.get();

            if (!user.isActive()) {
                throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
            }

            if (eventGuestRepository.existsByUserIdAndEvent(userId, event)) {
                return new MessageResponse("Este usuário já é um convidado deste evento.");
            }
        }

        Optional<EventInvitation> invitationOpt = eventInvitationRepository
                .findByEmailAddressAndEvent(emailAddress, event);

        if(invitationOpt.isPresent() && invitationOpt.get().notIsAccepted()) {
            invitationOpt.get().cancel();
            eventInvitationRepository.save(invitationOpt.get());
        }

        UUID guestId = userDtoOpt.map(UserDTO::id).orElse(null);

        EventInvitation invitation = EventInvitation.builder()
                .code(UUID.randomUUID())
                .event(event)
                .emailAddress(emailAddress)
                .status(EnumInvitationStatus.PENDING)
                .userId(guestId)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        eventInvitationRepository.save(invitation);

        return new MessageResponse("Um e-mail de convite foi enviado.");
    }

    public MessageResponse acceptInvite(UUID userId, UUID code) {
        EventInvitation invitation = eventInvitationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código não encontrado."));

        if(!invitation.isValid())
            throw new DomainException("Convite inválido");

        UserDTO userDTO = userManagement.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não cadastrado na plataforma"));

        if(invitation.isSameEmail(userDTO.emailAddress()))
            throw new AccessDeniedException("Usuário não autorizado.");

        invitation.accept();
        EventGuest guest = EventGuest.builder()
                .userId(userId)
                .event(invitation.getEvent())
                .build();

        eventInvitationRepository.save(invitation);
        eventGuestRepository.save(guest);

        return new MessageResponse("Convite aceito com sucesso.");
    }

    private Event getManageableEvent(UUID userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!canManageEvent(userId, event)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar enviar convites neste evento.");
        }

        return event;
    }

    private boolean canManageEvent(UUID userId, Event event) {
        return event.isOwner(userId)
                || editorRepository.existsByEventIdAndUserId(event.getId(), userId);
    }
}
