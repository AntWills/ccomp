package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EditorServices;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Gerir Eventos")
@RestController
@RequestMapping("api/events")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class EditorsController {
    private final EditorServices editorServices;

    public EditorsController(EditorServices editorServices) {
        this.editorServices = editorServices;
    }

    @Operation(
            summary = "Adiciona um editor ao evento",
            description = "Atribui um usuário como editor de um evento. **Restrito a membros da equipe (ADMIN, STAFF ou MODERATOR)** que também sejam os proprietários do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Editor adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (e-mail malformatado ou dados inconsistentes)"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Usuário comum ou não é o proprietário do evento)"),
            @ApiResponse(responseCode = "404", description = "Evento ou usuário não encontrado")
    })
    @PostMapping("/{eventId}/editors/{email}")
    public ResponseEntity<MessageResponse> addEditor(
            @PathVariable Long eventId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.addEditor(eventId, extractUserId(jwt), new EmailAddress(email));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Remove um editor do evento",
            description = "Remove as permissões de edição de um usuário sobre um evento. **Restrito a membros da equipe (ADMIN, STAFF ou MODERATOR)** que também sejam os proprietários do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Editor removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Usuário comum ou não é o proprietário do evento)"),
            @ApiResponse(responseCode = "404", description = "Evento ou editor não encontrado")
    })
    @DeleteMapping("{eventId}/editors/{email}")
    public ResponseEntity<MessageResponse> removeEditor(
            @PathVariable Long eventId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.removeEditor(eventId, extractUserId(jwt), new EmailAddress(email));
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
