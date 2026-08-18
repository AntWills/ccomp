package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class EditorServices {
    private final EventRepository eventRepository;
    private final EventEditorRepository editorRepository;
    private final UserManagement userManagement;

    public EditorServices(EventRepository eventRepository, EventEditorRepository editorRepository, UserManagement userManagement) {
        this.eventRepository = eventRepository;
        this.editorRepository = editorRepository;
        this.userManagement = userManagement;
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

        if (!userDTO.isTeamMember()) {
            throw new AccessDeniedException("Apenas membros da equipe (STAFF, MODERATOR ou ADMIN) podem ser adicionados como editores.");
        }

        if (editorRepository.existsByEventIdAndUserId(event.getId(), userDTO.id())) {
            return new MessageResponse("Este usuário já é um editor deste evento.");
        }

        editorRepository.save(EventEditor.builder()
                .event(event)
                .userId(userDTO.id())
                .assignedAt(LocalDateTime.now())
                .build());

        return new MessageResponse("Usuário adicionado como editor com sucesso.");
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
    boolean isEditor(Event event, UUID userId) {
        return editorRepository.existsByEventIdAndUserId(event.getId(), userId);
    }
}
