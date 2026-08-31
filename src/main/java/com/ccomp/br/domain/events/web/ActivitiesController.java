package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.ActivitiesServices;
import com.ccomp.br.domain.events.dto.activities.ActivityDTO;
import com.ccomp.br.domain.events.dto.activities.CreateActivityDTO;
import com.ccomp.br.domain.events.dto.activities.UpdateActivityDTO;
import com.ccomp.br.domain.events.dto.activities.EventActivityView;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Gerir Eventos (Atividades)")
@RestController
@RequestMapping("api/events")
public class ActivitiesController {
    private final ActivitiesServices activitiesServices;

    public ActivitiesController(ActivitiesServices activitiesServices) {
        this.activitiesServices = activitiesServices;
    }

    @Operation(summary = "Lista as atividades de um evento", description = "Retorna uma lista paginada (por cursor) de todas as atividades associadas a um evento específico. Acesso restrito a eventos abertos, dono do evento, editores ou administradores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atividades retornadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/{eventId}/activities")
    public ResponseEntity<CursorPage<EventActivityView>> getActivities(
            @PathVariable Long eventId,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(activitiesServices.searchByCursor(eventId, cursor, userId));
    }

    @Operation(summary = "Cria uma nova atividade", description = "Cria uma nova atividade para um evento específico. Requer autenticação do usuário e dados da atividade.")
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

        return ResponseEntity.status(HttpStatus.CREATED).body(activitiesServices.createActivity(userId, eventId, request));
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

        activitiesServices.deleteActivity(userId, id);
        return ResponseEntity.ok(new MessageResponse("Atividade removida com sucesso."));
    }

    @Operation(summary = "Atualiza uma atividade", description = "Atualiza os dados de uma atividade existente usando o seu ID. Requer autenticação do usuário.")
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

        return ResponseEntity.ok(activitiesServices.updateActivity(userId, id, request));
    }
}
