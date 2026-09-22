package com.ccomp.br.domain.events.guests.web;

import com.ccomp.br.domain.events.guests.application.GuestInvitationServices;
import com.ccomp.br.domain.events.guests.dto.InviteRequestDTO;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitation;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "events/guests/invitations (Convites)", description = "Gerenciamento de convidados e convites para eventos")
@RestController
@RequestMapping("/api/events")
public class GuestInvitationController {

    private final GuestInvitationServices guestInvitationServices;

    public GuestInvitationController(GuestInvitationServices guestInvitationServices) {
        this.guestInvitationServices = guestInvitationServices;
    }

    @GetMapping("/{eventId}/invitations")
    @Operation(summary = "Listar convites de um evento", description = "Retorna uma lista paginada (via cursor) de convites de um evento específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para visualizar convites deste evento"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    public ResponseEntity<CursorPage<EventInvitation>> searchInvitations(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        // Converte a string de email para o Value Object EmailAddress (caso exista na requisição)
        EmailAddress emailAddress = (email != null && !email.isBlank()) ? new EmailAddress(email) : null;

        CursorPage<EventInvitation> response = guestInvitationServices.searchInvitations(userId, emailAddress, eventId, cursor, pageSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/invitations")
    @Operation(summary = "Enviar um convite", description = "Gera um código e envia um convite para o e-mail informado para participar do evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite enviado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para gerenciar o evento")
    })
    public ResponseEntity<MessageResponse> inviteUser(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @Valid @RequestBody InviteRequestDTO request
    ) {
        EmailAddress emailAddress = new EmailAddress(request.email());
        MessageResponse response = guestInvitationServices.invite(userId, eventId, emailAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/{code}/reply")
    @Operation(summary = "Aceitar ou recusar convite", description = "Processa o código de um convite e aceita ou recusa baseado no payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite processado (aceito ou recusado)"),
            @ApiResponse(responseCode = "400", description = "Convite expirado ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário não autorizado a aceitar este convite (e-mail não coincide)"),
            @ApiResponse(responseCode = "404", description = "Código de convite não encontrado")
    })
    public ResponseEntity<MessageResponse> replyToInvite(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable UUID code,
            @Parameter(description = "Define se o convite será aceito (true) ou recusado (false). O valor padrão é true.")
            @RequestParam(defaultValue = "true") boolean accept
    ) {
        MessageResponse response = guestInvitationServices.acceptInvite(userId, code, accept);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @Operation(summary = "Cancelar convite enviado", description = "Cancela manualmente um convite previamente enviado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite cancelado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    public ResponseEntity<MessageResponse> cancelInvitation(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long invitationId
    ) {
        MessageResponse response = guestInvitationServices.cancelInvitation(userId, invitationId);
        return ResponseEntity.ok(response);
    }
}
