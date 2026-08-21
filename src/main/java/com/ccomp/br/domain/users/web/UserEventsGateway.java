package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.events.management.EventsManagement;
import com.ccomp.br.shared.dto.EventListItem;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Usuários / Eventos",
        description = "Endpoints para gerenciamento e consulta de eventos vinculados ao usuário autenticado.")
@RestController
@RequestMapping("api/users/me")
public class UserEventsGateway {
    private final EventsManagement eventsManagement;

    public UserEventsGateway(EventsManagement eventsManagement) {
        this.eventsManagement = eventsManagement;
    }

    @Operation(
            summary = "Lista os eventos criados pelo usuário",
            description = "Retorna uma lista contendo todos os eventos organizados/criados pelo usuário autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de eventos encontrados com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EventListItem.class))
                            )
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
    @GetMapping("created-events")
    public ResponseEntity<List<EventListItem>> getCreatedEvents(@AuthenticationPrincipal Jwt jwt){
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsManagement.findAllByOwnerId(ownerId));
    }

    @Operation(
            summary = "Lista as inscrições do usuário em eventos",
            description = "Retorna uma lista contendo todos os eventos nos quais o usuário logado se inscreveu como participante.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de inscrições encontrada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = EventListItem.class))
                            )
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
    @GetMapping("/events-subscriptions")
    public ResponseEntity<List<EventListItem>> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID participantId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsManagement.findAllSubscriptions(participantId));
    }
}
