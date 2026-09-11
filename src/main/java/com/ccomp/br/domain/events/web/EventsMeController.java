package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EventsServices;
import com.ccomp.br.domain.events.dto.events.EventListItemDTO;
import com.ccomp.br.shared.dto.EventListItemView;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Gerir Eventos (Me)")
@RestController
@RequestMapping("api/events/me")
public class EventsMeController {
    private final EventsServices eventsServices;

    public EventsMeController(EventsServices eventsServices) {
        this.eventsServices = eventsServices;
    }

    @Operation(
            summary = "Lista os eventos criados pelo usuário",
            description = "Retorna uma página contendo os eventos organizados/criados pelo usuário autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de eventos encontrados com sucesso."
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @GetMapping("created")
    public ResponseEntity<CursorPage<EventListItemDTO>> getCreatedEvents(
            @Parameter(description = "Cursor para carregar a próxima página")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsServices.findAllByOwnerId(ownerId, nextCursor, pageSize));
    }


    @GetMapping("/subscriptions")
    @Operation(
            summary = "Lista as inscrições do usuário em eventos",
            description = "Retorna uma página contendo todos os eventos nos quais o usuário logado se inscreveu como participante.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de inscrições encontrada com sucesso."
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<CursorPage<EventListItemDTO>> getSubscriptions(
            @Parameter(description = "Cursor para carregar a próxima página")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID participantId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsServices.findAllSubscriptions(participantId, nextCursor, pageSize));
    }

    @GetMapping("/editors")
    @Operation(
            summary = "Lista todos os eventos onde o usuário é editor",
            description = "Retorna uma página contendo todos os eventos nos quais o usuário logado se inscreveu como participante.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de edições encontrada com sucesso."
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<CursorPage<EventListItemDTO>> getMeEventsEditors(
            @Parameter(description = "Cursor para carregar a próxima página")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(eventsServices.findMyEditableEvents(extractUserId(jwt), nextCursor, pageSize));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
