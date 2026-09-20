package com.ccomp.br.domain.events.editors.application;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import com.ccomp.br.domain.events.editors.persistence.EventEditorDslRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.events.editors.persistence.validation.EventEditorInvitations;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccomp.br.domain.events.editors.dto.EventEditorCursor;
import com.ccomp.br.domain.events.editors.dto.EventEditorListItem;
import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import com.ccomp.br.domain.events.editors.persistence.validation.EventEditorInvitationsRepository;
import com.ccomp.br.domain.events.core.external.EditorAddedMessageDTO;

@Service
@Slf4j
public class EditorServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final EventEditorDslRepository editorDslRepository;
    private final UserManagement userManagement;
    private final RabbitTemplate rabbitTemplate;
    private final EventEditorInvitationsRepository invitationsRepository;

    public EditorServices(EventRepository eventRepository, EventEditorRepository editorRepository, UserManagement userManagement, ApplicationEventPublisher eventPublisher, EventEditorDslRepository editorDslRepository, RabbitTemplate rabbitTemplate, EventEditorInvitationsRepository invitationsRepository) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
        this.editorDslRepository = editorDslRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.invitationsRepository = invitationsRepository;
    }

    @Transactional
    public MessageResponse addEditor(Long eventId, UUID ownerId, EmailAddress emailAddress) {
        Event event = getEventAndValidateOwnership(eventId, ownerId);
        Optional<UserDTO> userDtoOpt = userManagement.findByEmailAddress(emailAddress);

        if (userDtoOpt.isPresent()) {
            UserDTO user = userDtoOpt.get();

            if (!user.isActive()) {
                throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
            }

            if (editorRepository.existsByEventIdAndUserId(eventId, user.id())) {
                return new MessageResponse("Este usuário já é um editor ativo deste evento.");
            }
        }

        reissueInvitation(event, emailAddress);
        return new MessageResponse("Um e-mail de convite foi enviado.");
    }

    @Transactional
    public MessageResponse acceptInvitation(UUID code, UUID userId) {
        EventEditorInvitations invitation = getInviteAndValid(code);

        UserDTO userDTO = userManagement.findByEmailAddress(invitation.getEmailAddress())
                .orElseThrow(() -> new UserNotFoundException("Usuário deve estar cadastrado no sistema."));

        if (!userDTO.isActive()) {
            throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
        }

        if(!userDTO.id().equals(userId))
            throw new AccessDeniedException("Este convite foi enviado para outro e-mail e não pertence à sua conta.");

        if (editorRepository.existsByEventIdAndUserId(invitation.getEventId(), userDTO.id())) {
            invitationsRepository.delete(invitation);
            return new MessageResponse("Você já é um editor ativo deste evento.");
        }

        Event event = eventRepository.findById(invitation.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        EventEditor editor = EventEditor.builder()
                .userId(userDTO.id())
                .event(event)
                .status(EnumEditorsStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();

        editorRepository.save(editor);
        invitationsRepository.delete(invitation);

        return new MessageResponse("Convite aceito com sucesso. Você agora é um editor do evento %s.".formatted(event.getTitle()));
    }

    @Transactional
    public MessageResponse removeEditor(Long eventId, UUID ownerId, EmailAddress emailAddress){
        Event event = getEventAndValidateOwnership(eventId, ownerId);

        UserDTO userDTO = userManagement.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado para o e-mail: %s".formatted(emailAddress)));

        if (!editorRepository.existsByEventIdAndUserId(event.getId(), userDTO.id())) {
            return new MessageResponse("O usuário não é editor deste evento.");
        }

        editorRepository.deleteByEventIdAndUserId(event.getId(), userDTO.id());

        return new MessageResponse("Usuário removido como editor.");
    }

    @Transactional(readOnly = true)
    public boolean hasPermissionEdit(Event event, UUID userId) {
        return editorRepository
                .findByEventIdAndUserId(event.getId(), userId)
                .map(EventEditor::isActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public CursorPage<EventEditorListItem> getEditorsByEvent(Long eventId, UUID requesterId, String cursor, int pageSize) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean canAccess = SecurityUtils.isAdmin()
                || event.isOwner(requesterId)
                || hasPermissionEdit(event, requesterId);

        if (!canAccess) {
            throw new AccessDeniedException("Você não tem permissão para visualizar os editores deste evento.");
        }

        int finalPageSize = Math.min(pageSize, 50);

        EventEditorCursor decodedCursor = CursorUtils.decode(cursor, EventEditorCursor.class);
        List<EventEditorListItem> results = editorDslRepository
                .findAllWithCursor(eventId, decodedCursor, finalPageSize + 1);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                ee -> new EventEditorCursor(ee.assignedAt(), ee.id())
        );
    }

    // ====================================================================================
    // MÉTODOS PRIVADOS
    // ====================================================================================

    private Event getEventAndValidateOwnership(Long eventId, UUID ownerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!event.isOwner(ownerId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar os editores deste evento.");
        }
        return event;
    }

    private EventEditorInvitations getInviteAndValid(UUID code) {
        return invitationsRepository.findByCode(code)
                .filter(inv -> !inv.isExpired())
                .orElseThrow(() -> new ResourceNotFoundException("Código de convite inválido ou expirado."));
    }

    private void reissueInvitation(Event event, EmailAddress emailAddress) {
        invitationsRepository.findByEmailAddressAndEventId(emailAddress, event.getId())
                .ifPresent(invitationsRepository::delete);

        UUID code = UUID.randomUUID();
        EventEditorInvitations newInvite = EventEditorInvitations.builder()
                .code(code)
                .eventId(event.getId())
                .emailAddress(emailAddress)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        invitationsRepository.save(newInvite);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_EDITOR_INVITATION,
                new EditorAddedMessageDTO(event.getId(), event.getTitle(), code, emailAddress)
        );
    }
}