package com.ccomp.br.domain.events.editors.application;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.external.message.EditorInvitationChannel;
import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import com.ccomp.br.domain.events.editors.persistence.EventEditorCache;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccomp.br.domain.events.editors.dto.EventEditorCursor;
import com.ccomp.br.domain.events.editors.dto.EventEditorListItem;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ccomp.br.domain.events.editors.persistence.validation.EventEditorInvitationsRepository;
import com.ccomp.br.domain.events.core.external.EditorAddedMessageDTO;

@Service
@Slf4j
public class EditorServices {
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final EventEditorRepository editorRepository;
    private final EventEditorDslRepository editorDslRepository;
    private final EventEditorCache eventEditorCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserManagement userManagement;
    private final RabbitTemplate rabbitTemplate;
    private final EventEditorInvitationsRepository invitationsRepository;

    public EditorServices(EventRepository eventRepository,
                          EventCache eventCache,
                          EventEditorRepository editorRepository,
                          UserManagement userManagement,
                          EventEditorDslRepository editorDslRepository,
                          EventEditorCache eventEditorCache, EventAccessPolicy eventAccessPolicy,
                          RabbitTemplate rabbitTemplate,
                          EventEditorInvitationsRepository invitationsRepository) {
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
        this.editorDslRepository = editorDslRepository;
        this.eventEditorCache = eventEditorCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.rabbitTemplate = rabbitTemplate;
        this.invitationsRepository = invitationsRepository;
    }

    @Transactional
    public MessageResponse addEditor(Long eventId, UUID ownerId, EmailAddress emailAddress) {
        EventDTO event = getEventAndValidateOwnership(eventId, ownerId);
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
        evictEditorCacheAfterCommit(invitation.getEventId(), userDTO.id());
        invitationsRepository.delete(invitation);

        return new MessageResponse("Convite aceito com sucesso. Você agora é um editor do evento %s.".formatted(event.getTitle()));
    }

    @Transactional
    public MessageResponse removeEditor(Long eventId, UUID ownerId, EmailAddress emailAddress){
        EventDTO event = getEventAndValidateOwnership(eventId, ownerId);

        UserDTO userDTO = userManagement.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado para o e-mail: %s".formatted(emailAddress)));

        if (editorRepository.findByEventIdAndUserId(event.id(), userDTO.id()).isEmpty())
            return new MessageResponse("O usuário não é editor deste evento.");

        editorRepository.deleteByEventIdAndUserId(event.id(), userDTO.id());
        evictEditorCacheAfterCommit(event.id(), userDTO.id());

        return new MessageResponse("Usuário removido como editor.");
    }

    @Transactional(readOnly = true)
    public CursorPage<EventEditorListItem> getEditorsByEvent(Long eventId, UUID requesterId, String cursor, int pageSize) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, requesterId)) {
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

    private EventDTO getEventAndValidateOwnership(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(event, userId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar os editores deste evento.");
        }
        return event;
    }

    private EventEditorInvitations getInviteAndValid(UUID code) {
        return invitationsRepository.findByCode(code)
                .filter(inv -> !inv.isExpired())
                .orElseThrow(() -> new ResourceNotFoundException("Código de convite inválido ou expirado."));
    }

    private void reissueInvitation(EventDTO event, EmailAddress emailAddress) {
        invitationsRepository.findByEmailAddressAndEventId(emailAddress, event.id())
                .ifPresent(invitationsRepository::delete);

        UUID code = UUID.randomUUID();
        EventEditorInvitations newInvite = EventEditorInvitations.builder()
                .code(code)
                .eventId(event.id())
                .emailAddress(emailAddress)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        invitationsRepository.save(newInvite);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                EditorInvitationChannel.ROUTING,
                new EditorAddedMessageDTO(event.id(), event.title(), code, emailAddress)
        );
    }

    private void evictEditorCacheAfterCommit(long eventId, UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventEditorCache.evict(eventId, userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventEditorCache.evict(eventId, userId);
            }
        });
    }
}
