package com.ccomp.br.domain.events.core.web;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.*;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "events", description = "Operações relacionadas à criação, busca, atualização e gestão do ciclo de vida dos eventos.")
@RestController
@RequestMapping("api/events")
public class EventsController {

    private final EventsServices eventsServices;

    public EventsController(EventsServices eventsServices) {
        this.eventsServices = eventsServices;
    }

    @Operation(
            summary = "Busca detalhes de um evento por ID",
            description = """
        Retorna os detalhes do evento identificado pelo ID.

        **Acesso:** eventos `PUBLISHED` podem ser consultados sem autenticação. Para `DRAFT`, `UNLISTED` ou
        `CANCELED`, envie um JWT pertencente ao proprietário, a um editor ativo, `MODERATOR` ou `ADMIN`.

        **Respostas:** `200` com `EventDTO`; `403` quando o evento existe, mas o usuário não tem acesso;
        `404` sem corpo quando o evento não existe.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento retornado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (evento em rascunho sem permissão suficiente)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "ID inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content
            )
    })
    @SecurityRequirements
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getById(
            @Parameter(description = "ID numérico do evento", example = "42") @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = (jwt == null) ? null : extractUserId(jwt);

        return eventsServices.getById(eventId, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Busca detalhes de um evento público por Slug",
            description = """
        Retorna as informações de um evento através do seu identificador amigável na URL (*slug*).

        ### Regras de Visibilidade:
        - Esta consulta é **estritamente pública** e restrita a eventos com status **PUBLISHED**.
        - Eventos no estado `DRAFT`, `UNLISTED` ou `CANCELED` **não** serão retornados por esta rota (retornará `404 Not Found`).
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento publicado retornado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado ou indisponível publicamente",
                    content = @Content
            )
    })
    @SecurityRequirements
    @GetMapping("/slug/{slug}")
    public ResponseEntity<EventDTO> getBySlug(@PathVariable String slug) {
        return eventsServices.getBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Busca e filtra eventos com paginação por cursor",
            description = """
        Realiza consultas no catálogo de eventos com suporte a paginação contínua (*Cursor-based Pagination*).

        O corpo aceita filtros opcionais `category` e `format`, além de `searchTerm`. A busca considera somente
        o título de eventos `PUBLISHED`; aceita o resultado quando `word_similarity(searchTerm, title) >= 0.4`.
        Com termo, ordena por `word_similarity + similarity` decrescente e desempata por `startDate` e ID
        decrescentes. Sem termo, ordena por `startDate` e ID decrescentes.

        O corpo JSON segue `EventsFilterRequest`; a resposta é `CursorPage<EventListItemDTO>` com `content`,
        `next_cursor` e `previous_cursor`.

        `pageSize` é um parâmetro de consulta opcional, padrão `10` e limitado a `50`. `nextCursor` é um token
        opaco retornado pela página anterior; continue usando o mesmo termo e os mesmos filtros. A resposta tem
        `content` (itens `EventListItemDTO`), `next_cursor` e `previous_cursor`.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de eventos retornada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CursorPage.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros de filtro inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirements
    @PostMapping("search")
    public ResponseEntity<CursorPage<EventListItemDTO>> searchEvents(
            @Valid @RequestBody EventsFilterRequest filter,
            @Parameter(description = "Cursor opaco retornado pela página anterior; use os mesmos filtros")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens solicitados na página; padrão 10 e máximo 50", example = "10")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(eventsServices.searchEventsWithFilters(filter, nextCursor, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'STAFF')")
    @Operation(
            summary = "Cria um novo evento",
            description = """
        Cria um novo evento atribuindo o status inicial como `DRAFT` (Rascunho).

        Envie `title`, `category` e `format`; `startDate` e `endDate` são opcionais. O evento é criado como
        `DRAFT`. Requer JWT de usuário com papel `ADMIN`, `MODERATOR` ou `STAFF`.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Evento criado em estado DRAFT",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de formulário ou regra de negócio violada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de autenticação ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "O usuário autenticado não tem papel ADMIN, MODERATOR ou STAFF",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EventDTO> create(
            @Valid @RequestBody CreateEventDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventsServices.create(extractUserId(jwt), dto));
    }

    @PatchMapping("{eventId}")
    @Operation(
            summary = "Atualiza parcialmente um evento existente",
            description = """
        Atualiza somente os campos enviados em `UpdateEventDTO`: `title`, `summary`, `content`, `category`, `format`,
        `scheduleConflictPolicy`, datas de realização e de inscrição, e `enrollmentPaused`. Campos omitidos ficam
        inalterados. Alterar título também regenera o slug. Capa e status têm endpoints próprios.

        Requer JWT do proprietário, de um editor ativo, `MODERATOR` ou `ADMIN`.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento atualizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "ID inválido ou corpo malformado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário sem permissão de edição",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EventDTO> updateEvent(
            @Valid @RequestBody UpdateEventDTO request,
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(eventsServices.update(request, eventId, extractUserId(jwt)));
    }

    @PatchMapping("{eventId}/status/{status}")
    @Operation(
            summary = "Atualiza o status de publicação de um evento",
            description = """
        Transiciona o evento para o status informado no caminho: `DRAFT`, `PUBLISHED`, `UNLISTED` ou `CANCELED`.
        As regras de domínio podem rejeitar a transição, por exemplo, publicar sem datas. Requer JWT do proprietário,
        de um editor ativo, `MODERATOR` ou `ADMIN`.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status alterado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID/status inválido ou transição rejeitada por regra de domínio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (Usuário não possui permissão de edição no evento)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<MessageResponse> updateEventStatus(
            @PathVariable Long eventId,
            @PathVariable EnumEventStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(eventsServices.updateEventStatus(eventId, status, extractUserId(jwt)));
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Exclui um evento por ID", description = "Exclui o evento e seus dados dependentes. Requer JWT do proprietário, de um editor ativo, MODERATOR ou ADMIN. A resposta de sucesso não tem corpo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        eventsServices.delete(eventId, extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
