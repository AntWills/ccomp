package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EditorServices;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Adiciona um editor ao evento", description = "Atribui um usuário como editor de um evento. Requer autenticação do dono do evento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Editor adicionado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento ou usuário não encontrado")
    })
    @PostMapping("/{eventId}/editors/{userId}")
    public ResponseEntity<MessageResponse> addEditor(
            @PathVariable Long eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.addEditor(eventId, UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove um editor do evento", description = "Remove as permissões de edição de um usuário sobre um evento. Requer autenticação do dono do evento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Editor removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento ou usuário não encontrado")
    })
    @DeleteMapping("{eventId}/editors/{userId}")
    public ResponseEntity<MessageResponse> removeEditor(
            @PathVariable Long eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt){
        MessageResponse response = editorServices.removeEditor(eventId, UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.ok(response);
    }
}
