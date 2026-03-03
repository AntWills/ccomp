package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EventsApplication;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Gerir Eventos", description = "Operações relacionadas a criação, busca, atulização e deleção de eventos.")
@RestController
@RequestMapping("/events")
public class EventsController {
    private final EventsApplication eventsApplication;

    public EventsController(EventsApplication eventsApplication) {
        this.eventsApplication = eventsApplication;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventsApplication.create(UUID.fromString(jwt.getSubject()), dto));
    }

    @Operation(
            summary = "Busca detalhes de um evento por ID",
            description = """
        **Regras de acesso para esta rota**
        
        ## 1. Evento ABERTO (público)
        - Acessível por **qualquer pessoa** (autenticada ou anônima)  
        → **200 OK** + dados do evento
        
        ## 2. Evento FECHADO (privado)
        - Apenas o **proprietário** pode acessar  
        - Exige autenticação **e** propriedade  
        → **200 OK** + dados do evento
        
        ### Acesso negado
        - Usuário autenticado **mas não dono**  
        - Usuário **não autenticado**  
        → **403 Forbidden**
        
        ## 3. Evento não existe
        → **404 Not Found** (ou null)
        
        **Observação**  
        Autenticação **opcional** via Bearer JWT.  
        Verificada somente em eventos fechados.
        """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Evento encontrado e acesso permitido",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = EventResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não aplicável nesta rota (rota pública)",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Acesso negado: evento fechado e usuário não é o proprietário (ou não autenticado)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class) // se você tiver um DTO de erro
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Evento não encontrado",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = jwt == null ? null : UUID.fromString(jwt.getSubject());

        return eventsApplication.getById(eventId, userId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
