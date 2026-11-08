package com.ccomp.br.domain.events.activities.web;

import java.util.Optional;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.dto.*;
import com.ccomp.br.domain.events.activities.application.ActivityServices;
import com.ccomp.br.domain.events.shared.dto.CapacityUpdateRequest;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "events/activities (Atividades)")
@RestController
@RequestMapping("api/events")
public class ActivitiesController {
    private final ActivityServices activityServices;

    public ActivitiesController(ActivityServices activityServices) {
        this.activityServices = activityServices;
    }

    @Operation(summary = "Lista as atividades de um evento", description = "Retorna uma lista paginada (por cursor) de todas as atividades associadas a um evento específico. start_date e end_date são horários locais interpretados no time_zone do evento pai; a resposta inclui time_zone. Acesso restrito a eventos abertos, dono do evento, editores ou administradores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atividades retornadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/{eventId}/activities")
    @SecurityRequirements
    public ResponseEntity<CursorPage<ActivityDTO>> getActivities(
            @PathVariable Long eventId,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(activityServices.searchByCursor(eventId, cursor, userId));
    }

    @Operation(summary = "Cria uma nova atividade", description = "Cria uma nova atividade para um evento específico. `maxCapacity` é opcional, deve ser maior ou igual a zero e null significa sem limite. Uma atividade pública com capacidade definida exige inscrição para reservar vaga. O cronograma é informado ao atualizar a atividade; start_date e end_date são horários locais no time_zone do evento pai e não devem conter offset. As respostas incluem time_zone e maxCapacity. Requer autenticação e permissão de edição.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atividade criada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PostMapping("/{eventId}/activities")
    public ResponseEntity<ActivityDTO> createActivity(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateActivityDTO request, @AuthenticationPrincipal Jwt jwt){
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.status(HttpStatus.CREATED).body(activityServices.createActivity(userId, eventId, request));
    }

    @Operation(summary = "Deleta uma atividade", description = "Remove uma atividade existente usando o seu ID. Requer autenticação do usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atividade removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada")
    })
    @DeleteMapping("/activities/{id}")
    public ResponseEntity<MessageResponse> deleteActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        activityServices.deleteActivity(userId, id);
        return ResponseEntity.ok(new MessageResponse("Atividade removida com sucesso."));
    }

    @Operation(summary = "Atualiza uma atividade", description = "Atualiza os dados de uma atividade existente, incluindo `maxCapacity` (inteiro maior ou igual a zero). Null ou omissão no PATCH preserva o valor; use PUT /api/events/activities/{id}/capacity com null para remover o limite. Uma atividade pública com capacidade definida exige inscrição para reservar vaga. start_date e end_date são horários locais no time_zone do evento pai; não envie offset. Campos de capacidade só podem ser alterados pela equipe do evento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atividade atualizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para atualizar a atividade"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada")
    })
    @PatchMapping("/activities/{id}")
    public ResponseEntity<ActivityDTO> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateActivityDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.ok(activityServices.updateActivity(userId, id, request));
    }

    @PutMapping("/activities/{id}/capacity")
    @Operation(summary = "Define ou remove a capacidade máxima da atividade",
            description = "Envia um inteiro maior ou igual a zero para limitar inscrições ativas ou null para remover o limite. O valor também aparece nas respostas de consulta das atividades. Requer owner, editor ativo, moderator ou admin do evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capacidade atualizada"),
            @ApiResponse(responseCode = "400", description = "Capacidade deve ser maior ou igual a zero ou null"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o evento"),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado")
    })
    public ResponseEntity<ActivityDTO> updateActivityCapacity(@PathVariable Long id,
            @Valid @RequestBody CapacityUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = Optional.ofNullable(jwt).map(Jwt::getSubject).map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
        return ResponseEntity.ok(activityServices.updateCapacity(userId, id, request));
    }
}
