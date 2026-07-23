package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
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
    public MessageResponse addEditor(Long eventId, UUID ownerId, UUID userId){
//        log.info("addEditor chamado | eventId={}, ownerId={}, userId={}",
//                eventId, ownerId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(ownerId)) throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        if(editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            return new MessageResponse("O usuario já é editor deste evento");

        if(!userManagement.userExists(userId))
            throw new UserNotFaundException("O usuario não existe.");

        editorRepository.save(EventEditor.builder()
                .event(event)
                .userId(userId)
                .assignedAt(LocalDateTime.now())
                .build());

        return new MessageResponse("Usuario adicionar como editor.");
    }

    @Transactional
    public MessageResponse removeEditor(Long eventId, UUID ownerId, UUID userId){
//        log.info("removerEditor chamado | eventId={}, ownerId={}, userId={}",
//                eventId, ownerId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(ownerId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        if(!editorRepository.existsByEventIdAndUserId(event.getId(), userId))
            return new MessageResponse("O usuário não é editor deste evento.");

        editorRepository.deleteByEventIdAndUserId(event.getId(), userId);

        return new MessageResponse("Usuário removido como editor.");
    }
}
