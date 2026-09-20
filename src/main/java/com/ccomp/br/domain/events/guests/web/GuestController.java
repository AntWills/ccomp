package com.ccomp.br.domain.events.guests.web;

import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
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

@RestController
@RequestMapping("/api/events")
@Tag(name = "Gerir Eventos (Convidados)", description = "Gestão da lista de convidados de eventos e atividades")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @GetMapping("/{eventId}/guests")
    @Operation(summary = "Buscar convidados do evento", description = "Retorna de forma paginada (cursor) os convidados do evento.")
    public ResponseEntity<CursorPage<EventGuest>> searchEventGuests(
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable Long eventId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(guestService.searchEventGuests(eventId, userId, cursor, pageSize));
    }

    @GetMapping("/activities/{activityId}/guests")
    @Operation(summary = "Buscar convidados da atividade", description = "Retorna de forma paginada (cursor) os convidados vinculados à atividade.")
    public ResponseEntity<CursorPage<ActivityGuest>> searchActivityGuests(
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable Long activityId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(guestService.searchActivityGuests(activityId, userId, cursor, pageSize));
    }

    @PatchMapping("/{eventId}/guests/{guestId}")
    @Operation(summary = "Atualizar status e visibilidade do convidado", description = "Permite ao próprio convidado ou gerente do evento alterar presença e privacidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convidado atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para alterar este convidado")
    })
    public ResponseEntity<MessageResponse> updateGuest(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody UpdateGuestDTO request
    ) {
        guestService.updateEventGuest(eventId, guestId, userId, request);
        return ResponseEntity.ok(new MessageResponse("Preferências do convidado atualizadas com sucesso."));
    }
}
