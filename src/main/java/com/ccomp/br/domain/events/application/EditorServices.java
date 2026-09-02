package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
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
import com.ccomp.br.domain.events.persistence.editors.validation.EditorValidationCodeRepository;
import com.ccomp.br.domain.events.persistence.editors.validation.EditorValidationCode;
import com.ccomp.br.domain.events.external.dto.EditorAddedEvent;
import com.ccomp.br.shared.exceptions.DomainException;

@Service
@Slf4j
public class EditorServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final UserManagement userManagement;
    private final ApplicationEventPublisher eventPublisher;
    private final EditorValidationCodeRepository validationCodeRepository;

    public EditorServices(EventRepository eventRepository, EventEditorRepository editorRepository, UserManagement userManagement, ApplicationEventPublisher eventPublisher, EditorValidationCodeRepository validationCodeRepository) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
        this.eventPublisher = eventPublisher;
        this.validationCodeRepository = validationCodeRepository;
    }

    @Transactional
    public MessageResponse addEditor(Long eventId, UUID ownerId, EmailAddress emailAddress) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!event.isOwner(ownerId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar os editores deste evento.");
        }

        UserDTO userDTO = userManagement.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado para o e-mail: %s".formatted(emailAddress)));

        if (!userDTO.isActive()) {
            throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
        }

        Optional<EventEditor> editorOpt = editorRepository.findByEventIdAndUserId(event.getId(), userDTO.id());

        EventEditor editor;

        if (editorOpt.isPresent()) {
            editor = editorOpt.get();

            if (editor.isActive()) {
                return new MessageResponse("Este usuário já é um editor ativo deste evento.");
            }

            // Caso seja PENDING ou REVOKED, reativa/mantém como PENDING e indica o reenvio
            editor.setStatus(EnumEditorsStatus.PENDING);
            editor = editorRepository.save(editor);

            validationCodeRepository.deleteByEventEditor(editor);
        } else {
            editor = editorRepository.save(EventEditor.builder()
                    .event(event)
                    .userId(userDTO.id())
                    .status(EnumEditorsStatus.PENDING)
                    .assignedAt(LocalDateTime.now())
                    .build());
        }

        UUID code = UUID.randomUUID();

        EditorValidationCode validationCode = EditorValidationCode.builder()
                .code(code)
                .eventEditor(editor)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        validationCodeRepository.save(validationCode);

        eventPublisher.publishEvent(new EditorAddedEvent(event.getId(), event.getTitle(), code, userDTO, editor.getId()));

        return new MessageResponse("Usuário adicionado como editor com sucesso. Um e-mail de convite foi enviado.");
    }

    @Transactional
    public MessageResponse acceptInvitation(UUID code) {
        EditorValidationCode validationCode = validationCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código de convite inválido ou não encontrado."));

        if (validationCode.isExpired()) {
            throw new DomainException("O código de convite expirou.");
        }

        EventEditor editor = validationCode.getEventEditor();

        if (editor.isActive()) {
            validationCodeRepository.delete(validationCode);
            return new MessageResponse("Você já é um editor ativo deste evento.");
        }

        editor.setStatus(EnumEditorsStatus.ACTIVE);
        editorRepository.save(editor);

        validationCodeRepository.deleteByEventEditor(editor);

        return new MessageResponse("Convite aceito com sucesso. Você agora é um editor do evento.");
    }

    @Transactional
    public MessageResponse removeEditor(Long eventId, UUID ownerId, EmailAddress emailAddress){
//        log.info("removerEditor chamado | eventId={}, ownerId={}, userId={}",
//                eventId, ownerId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(ownerId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        UserDTO userDTO = userManagement.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado para o e-mail: %s".formatted(emailAddress)));

        if(!editorRepository.existsByEventIdAndUserId(event.getId(), userDTO.id()))
            return new MessageResponse("O usuário não é editor deste evento.");

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

        EventEditorCursor decodedCursor = CursorUtils.decode(cursor, EventEditorCursor.class).orElse(null);
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
}
