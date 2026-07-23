package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EditorServices;
import com.ccomp.br.domain.events.application.EventsServices;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Gerir Eventos")
@RestController
@RequestMapping("api/events")
public class EditorsController {
    private final EditorServices editorServices;

    public EditorsController(EditorServices editorServices) {
        this.editorServices = editorServices;
    }

    @PostMapping("/{eventId}/editors/{userId}")
    public ResponseEntity<MessageResponse> addEditor(
            @PathVariable Long eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.addEditor(eventId, UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("{eventId}/editors/{userId}")
    public ResponseEntity<MessageResponse> removeEditor(
            @PathVariable Long eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.removeEditor(eventId, UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.ok(response);
    }
}
