package com.ccomp.br.domain.events.core.web;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
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

@Tag(name = "events/me", description = "Listagens autenticadas dos eventos relacionados ao usuário atual")
@RestController
@RequestMapping("api/events/me")
public class EventsMeController {
    private final EventsServices eventsServices;

    public EventsMeController(EventsServices eventsServices) {
        this.eventsServices = eventsServices;
    }

    @Operation(
            summary = "Lista os eventos criados pelo usuário",
            description = """
                    Retorna eventos cujo ownerId corresponde ao usuário do JWT, sem filtrar status. Ordena por
                    startDate e ID decrescentes. Retorna CursorPage com content, next_cursor e previous_cursor.
                    pageSize tem padrão 10 e máximo 50; nextCursor é opaco e vem da página anterior.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de eventos do usuário",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CursorPage.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Cursor inválido ou pageSize inválido",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("created")
    public ResponseEntity<CursorPage<EventListItemDTO>> getCreatedEvents(
            @Parameter(description = "Cursor opaco da página anterior", example = "eyJzdGFydERhdGUiOiIyMDI2LTAxLTAxVDAwOjAwIiwiaWQiOjQyfQ")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens por página; padrão 10, máximo 50", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsServices.findAllByOwnerId(ownerId, nextCursor, pageSize));
    }


    @GetMapping("/subscriptions")
    @Operation(
            summary = "Lista as inscrições do usuário em eventos",
            description = """
                    Retorna eventos em que o usuário do JWT possui inscrição CONFIRMED ou CHECKED_IN; não filtra
                    pelo status do evento. Outras situações de inscrição não entram na lista. Ordena por startDate e
                    ID decrescentes e retorna CursorPage
                    com content, next_cursor e previous_cursor. pageSize tem padrão 10 e máximo 50.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de eventos inscritos",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CursorPage.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Cursor inválido ou pageSize inválido",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<CursorPage<EventListItemDTO>> getSubscriptions(
            @Parameter(description = "Cursor opaco da página anterior")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens por página; padrão 10, máximo 50", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID participantId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsServices.findAllSubscriptions(participantId, nextCursor, pageSize));
    }

    @GetMapping("/editors")
    @Operation(
            summary = "Lista todos os eventos onde o usuário é editor",
            description = """
                    Retorna eventos em que o usuário do JWT tem vínculo de editor ACTIVE. Convites pendentes ou
                    revogados não aparecem. Ordena por startDate e ID decrescentes e retorna CursorPage com content,
                    next_cursor e previous_cursor. pageSize tem padrão 10 e máximo 50.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de eventos editáveis",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CursorPage.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado - Token ausente, inválido ou expirado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Cursor inválido ou pageSize inválido",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<CursorPage<EventListItemDTO>> getMeEventsEditors(
            @Parameter(description = "Cursor opaco da página anterior")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens por página; padrão 10, máximo 50", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(eventsServices.findMyEditableEvents(extractUserId(jwt), nextCursor, pageSize));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
