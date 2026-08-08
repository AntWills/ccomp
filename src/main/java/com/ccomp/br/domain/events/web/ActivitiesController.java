package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.ActivitiesServices;
import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
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

@Tag(name = "Gerir Eventos")
@RestController
@RequestMapping("api/events")
public class ActivitiesController {
    private final ActivitiesServices activitiesServices;

    public ActivitiesController(ActivitiesServices activitiesServices) {
        this.activitiesServices = activitiesServices;
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
            @Valid @RequestBody CreateActivityRequest request, @AuthenticationPrincipal Jwt jwt){
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
}
