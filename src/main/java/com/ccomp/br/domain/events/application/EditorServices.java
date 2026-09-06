package com.ccomp.br.domain.events.application;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.persistence.editors.validation.EventEditorInvitations;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
import com.ccomp.br.shared.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccomp.br.domain.events.dto.editors.EventEditorCursor;
import com.ccomp.br.domain.events.dto.editors.EventEditorListItem;
import com.ccomp.br.domain.events.persistence.editors.EventEditorSpec;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import com.ccomp.br.domain.events.persistence.editors.validation.EventEditorInvitationsRepository;
import com.ccomp.br.domain.events.external.dto.EditorAddedMessageDTO;

@Service
@Slf4j
public class EditorServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final UserManagement userManagement;
    private final RabbitTemplate rabbitTemplate;
    private final EventEditorInvitationsRepository invitationsRepository;

    public EditorServices(EventRepository eventRepository, EventEditorRepository editorRepository, UserManagement userManagement, ApplicationEventPublisher eventPublisher, RabbitTemplate rabbitTemplate, EventEditorInvitationsRepository invitationsRepository) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
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

        int maxPageSize = 50;
        int finalPageSize = pageSize > maxPageSize ? maxPageSize : pageSize;

        EventEditorCursor decodedCursor = CursorUtils.decode(cursor, EventEditorCursor.class);
        Specification<EventEditor> spec = EventEditorSpec.buildSpec(eventId, decodedCursor);

        List<EventEditor> results = editorRepository.findBy(spec, query -> query
                .limit(finalPageSize + 1)
                .sortBy(Sort.by(
                        Sort.Order.desc("assignedAt"),
                        Sort.Order.desc("id")
                ))
                .all());

        boolean hasNext = results.size() > finalPageSize;
        List<EventEditor> page = hasNext ? results.subList(0, finalPageSize) : results;

        List<UUID> ids = page.stream()
                .map(EventEditor::getUserId)
                .toList();

        Map<UUID, UserSummaryView> userMap = userManagement.findAllSummaryByIds(ids)
                .stream()
                .collect(Collectors.toMap(UserSummaryView::getId, Function.identity(), (user1, user2) -> user1));

        String nextCursor = hasNext && !page.isEmpty()
                ? CursorUtils.encode(new EventEditorCursor(page.getLast().getAssignedAt(), page.getLast().getId()))
                : null;

        List<EventEditorListItem> contents = page.stream()
                .map(ee -> EventEditorListItem.builder()
                        .id(ee.getId())
                        .eventId(ee.getEvent().getId())
                        .user(userMap.get(ee.getUserId()))
                        .assignedAt(ee.getAssignedAt())
                        .revokedAt(ee.getRevokedAt())
                        .status(ee.getStatus())
                        .build())
                .toList();

        return new CursorPage<>(contents, nextCursor, null);
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