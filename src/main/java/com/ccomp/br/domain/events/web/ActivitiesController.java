package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.ActivitiesEnrollmentsServices;
import com.ccomp.br.domain.events.application.ActivitiesServices;
import com.ccomp.br.domain.events.dto.activities.ActivityDTO;
import com.ccomp.br.domain.events.dto.activities.CreateActivityDTO;
import com.ccomp.br.domain.events.dto.activities.UpdateActivityDTO;
import com.ccomp.br.domain.events.dto.activities.EventActivityView;
import com.ccomp.br.domain.events.dto.enrollments.UserActivitySummaryDTO;
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
    private final ActivitiesEnrollmentsServices activitiesEnrollmentsServices;

    public ActivitiesController(ActivitiesServices activitiesServices, ActivitiesEnrollmentsServices activitiesEnrollmentsServices) {
        this.activitiesServices = activitiesServices;
        this.activitiesEnrollmentsServices = activitiesEnrollmentsServices;
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

    @Operation(summary = "Inscreve-se em uma atividade", description = "Realiza a inscrição do usuário autenticado em uma atividade específica. O usuário já deve estar inscrito no evento principal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscrição na atividade realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito (Usuário não inscrito no evento ou já inscrito na atividade)")
    })
    @PostMapping("/activities/{activityId}/subscribe")
    public ResponseEntity<MessageResponse> subscribeToActivity(
            @PathVariable Long activityId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.ok(activitiesEnrollmentsServices.inscribe(userId, activityId));
    }

    @Operation(summary = "Cancela inscrição em uma atividade", description = "Remove a inscrição do usuário autenticado em uma atividade específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscrição removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Inscrição na atividade não encontrada")
    })
    @DeleteMapping("/activities/{activityId}/subscribe")
    public ResponseEntity<MessageResponse> unsubscribeFromActivity(
            @PathVariable Long activityId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.ok(activitiesEnrollmentsServices.unsubscribe(userId, activityId));
    }

    @Operation(
            summary = "Lista os inscritos de uma atividade",
            description = "Retorna uma lista paginada (por cursor) dos usuários inscritos em uma atividade específica. O acesso é restrito a administradores do sistema, ao dono do evento ou aos editores autorizados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de inscritos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Requer permissão de gerência no evento)"),
            @ApiResponse(responseCode = "404", description = "Atividade ou Evento não encontrado")
    })
    @GetMapping("/activities/{activityId}/enrollments")
    public ResponseEntity<CursorPage<UserActivitySummaryDTO>> getActivityEnrollments(
            @PathVariable Long activityId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = Optional.ofNullable(jwt)
                .map(Jwt::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));

        return ResponseEntity.ok(
                activitiesEnrollmentsServices.findAllUsersFromActivity(userId, activityId, cursor, pageSize)
        );
    }
}
