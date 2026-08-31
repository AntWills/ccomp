package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EventsServices;
import com.ccomp.br.domain.events.dto.events.CreateEventDTO;
import com.ccomp.br.domain.events.dto.events.EventDTO;
import com.ccomp.br.domain.events.dto.events.EventsFilterRequest;
import com.ccomp.br.domain.events.dto.events.UpdateEventDTO;
import com.ccomp.br.shared.dto.EventListItem;
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

@Tag(name = "Gerir Eventos", description = "Operações relacionadas à criação, busca, atualização e gestão do ciclo de vida dos eventos.")
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
        Exibe as informações detalhadas de um evento respeitando suas regras de publicação e permissão.

        ### Regras de Acesso:
        1. **Evento Público ou Oculto (PUBLISHED / UNLISTED):**
           - Acessível por qualquer cliente (**público / anônimo** ou **autenticado**).
           - `-> 200 OK`

        2. **Evento Restrito (DRAFT / CANCELED):**
           - Acessível apenas se o usuário estiver **autenticado** E for o **Proprietário**, **Editor** atribuído ou **ADMIN**.
           - `-> 200 OK`

        ### Falhas e Exceções:
        - **403 Forbidden:** Evento privado/rascunho sem autorização.
        - **404 Not Found:** ID de evento inexistente.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento encontrado e acesso permitido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (evento em rascunho sem permissão suficiente)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getById(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
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
                    description = "Evento público encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado ou indisponível publicamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
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

        ### Comportamento da Paginação:
        - **`pageSize`:** Limite de itens por página (máximo ajustado automaticamente para 50).
        - **`nextCursor`:** Token para navegação contínua da próxima página.
        - **Ordenação:** Ordenado primariamente por `startDate DESC`.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de eventos retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros de filtro inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirements
    @PostMapping("search")
    public ResponseEntity<CursorPage<EventListItem>> searchEvents(
            @Valid @RequestBody EventsFilterRequest filter,
            @Parameter(description = "Cursor para carregar a próxima página")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(eventsServices.searchEventsWithFilters(filter, nextCursor, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Cria um novo evento",
            description = """
        Cria um novo evento atribuindo o status inicial como `DRAFT` (Rascunho).

        ### Requisitos de Permissão:
        - Requer autenticação do usuário.
        - O criador deve ter a role **ADMIN** ou **STAFF**.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Evento criado com sucesso"
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
                    description = "Acesso negado (Usuário não pertence à equipe)",
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Atualiza parcialmente um evento existente",
            description = """
        Permite alterar dados gerais do evento, como alterar status de publicação, datas de inscrições ou período de realização.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento atualizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (Usuário não é Administrador, Proprietário ou Editor)",
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

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Exclui um evento por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
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