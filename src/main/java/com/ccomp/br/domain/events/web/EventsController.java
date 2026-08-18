package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EventsServices;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.dto.EventListItem;
import com.ccomp.br.domain.events.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.dto.UpdateEventRequest;
import com.ccomp.br.shared.dto.EventResponse;
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

@Tag(name = "Gerir Eventos", description = "Operações relacionadas à criação, busca, atualização e gestão de acesso aos eventos.")
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
        Exibe as informações completas de um evento conforme o seu nível de visibilidade e as permissões do usuário.

        ### Regras de Acesso:
        1. **Evento ABERTO (Público):**
           - Acessível por qualquer cliente (**público / anônimo** ou **autenticado**).
           - `-> 200 OK`

        2. **Evento FECHADO (Privado):**
           - Acessível se o usuário estiver **autenticado** E for o **Proprietário** ou um **ADMIN**.
           - `-> 200 OK`

        ### Falhas e Exceções:
        - **403 Forbidden:** Evento fechado e o usuário não é proprietário nem ADMIN (ou requisição anônima).
        - **404 Not Found:** ID de evento inexistente.
        
        *Nota: O cabeçalho Bearer Token é opcional nesta rota e só é avaliado em eventos fechados.*
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento encontrado e acesso permitido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (evento fechado sem permissão de acesso)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento ou Usuário não encontrado no sistema",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirements
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = (jwt == null) ? null : extractUserId(jwt);

        return eventsServices.getById(eventId, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Busca e filtra eventos com paginação por cursor",
            description = """
        Realiza consultas avançadas no catálogo de eventos com suporte a paginação contínua (*Cursor-based Pagination*).

        ### Comportamento da Paginação:
        - **`pageSize`:** Define a quantidade de itens retornado por página. O valor é **limitado a no máximo 50 itens** (valores maiores serão reajustados para 50 automaticamente no backend).
        - **`nextCursor`:** Cursor hash retornado na resposta anterior para carregar a próxima página. Se nulo, busca a primeira página.
        - **Ordenação:** Garantida por `startDate DESC` seguido de `id DESC` como critério de desempate.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de eventos retornada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CursorPage.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros de filtro ou cursor em formato inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirements
    @PostMapping("search")
    public ResponseEntity<CursorPage<EventListItem>> searchEvents(
            @Valid @RequestBody EventsFilterRequest filter,
            @Parameter(description = "Cursor para carregar a próxima página (retornado em 'nextCursor' na busca anterior)")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(eventsServices.searchEventsWithFilters(filter, nextCursor, pageSize));
    }

    @Operation(
            summary = "Cria um novo evento",
            description = """
        Registra um novo evento associando o usuário autenticado como o proprietário (*owner*).

        ### Requisitos de Permissão:
        - Requer autenticação do usuário (**Bearer Token**).
        - **Restrito a membros da equipe:** O usuário criador deve obrigatoriamente ter a role **ADMIN** ou **STAFF**.
        - O slug amigável da URL é gerado automaticamente a partir do título enviado.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Evento criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Corpo da requisição inválido ou regras de validação violadas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de autenticação ausente ou expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (usuário autenticado não pertence à equipe STAFF/ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventsServices.create(extractUserId(jwt), dto));
    }

    @Operation(
            summary = "Atualiza parcialmente um evento existente",
            description = """
        Atualiza as propriedades do evento com base nos campos informados no corpo da requisição.

        ### Regras de Permissão e Edição:
        A alteração é permitida caso o usuário atenda a **pelo menos um** dos critérios abaixo:
        1. Ser um administrador (**ADMIN**).
        2. Ser o proprietário do evento (**Owner**).
        3. Estar atribuído como um editor ativo do evento (**Editor**).

        ### Efeito automático:
        - Caso o **título** do evento seja alterado na requisição, um **novo slug exclusivo** será recalculado automaticamente e associado ao evento.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Evento atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventListItem.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de atualização inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (Usuário não é ADMIN, Proprietário ou Editor atribuído ao evento)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento ou Usuário não encontrado no banco de dados",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EventListItem> updateEvent(
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(eventsServices.update(request, extractUserId(jwt)));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}