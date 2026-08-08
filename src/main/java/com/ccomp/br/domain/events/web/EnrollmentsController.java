package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EnrollmentsServices;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class EnrollmentsController {
    private final EnrollmentsServices enrollmentsServices;

    public EnrollmentsController(EnrollmentsServices enrollmentsServices) {
        this.enrollmentsServices = enrollmentsServices;
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
