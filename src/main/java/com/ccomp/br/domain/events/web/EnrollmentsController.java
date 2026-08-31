package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EnrollmentsServices;
import com.ccomp.br.domain.events.dto.enrollments.EnrollmentListItem;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Gerir Eventos (Inscritos)")
@RestController
@RequestMapping("api/events")
public class EnrollmentsController {
    private final EnrollmentsServices enrollmentsServices;

    public EnrollmentsController(EnrollmentsServices enrollmentsServices) {
        this.enrollmentsServices = enrollmentsServices;
    }

    @Operation(
            summary = "Lista os inscritos de um evento com paginação por cursor",
            description = """
        Retorna a listagem dos participantes inscritos no evento de forma paginada (*Cursor-based Pagination*).

        ### Comportamento e Detalhes da Resposta:
        - **`pageSize`:** Quantidade de registros retornados por página (limite ajustado automaticamente para no máximo 50).
        - **`nextCursor`:** Hash de paginação enviado para carregar os próximos inscritos.
        - **Dados do Inscrito:** Cada item traz os dados de auditoria da inscrição (`id`, `status`, `createdAt`) e a visão resumida do perfil do participante (`UserSummaryView`).
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de inscritos retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (Requer papel STAFF ou ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{eventId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CursorPage<EnrollmentListItem>> searchEnrollments(
            @PathVariable Long eventId,
            @Parameter(description = "Cursor para carregar a próxima página (retornado em 'nextCursor' na busca anterior)")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Quantidade de registros por página (Padrão: 10, Máximo: 50)")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(enrollmentsServices.searchEnrollments(eventId, nextCursor, pageSize));
    }

    private UUID extractUserId(Jwt jwt) {
        return Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
    }

    @Operation(summary = "Inscreve-se em um evento", description = "Realiza a inscrição do usuário autenticado no evento informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscrição realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PostMapping("/{eventId}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentsServices.subscribe(userId, eventId));
    }

    @Operation(summary = "Cancela inscrição em um evento", description = "Remove a inscrição do usuário autenticado no evento informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscrição cancelada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @DeleteMapping("/{eventId}/subscribe")
    public ResponseEntity<?> unsubscribe(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.status(HttpStatus.OK)
                .body(enrollmentsServices.unsubscribe(userId, eventId));
    }
}
