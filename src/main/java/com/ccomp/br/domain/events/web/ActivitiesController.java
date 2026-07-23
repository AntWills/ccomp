package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.ActivitiesServices;
import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Gerir Eventos")
@RestController
@RequestMapping("api/events")
public class ActivitiesController {
    private final ActivitiesServices activitiesServices;

    public ActivitiesController(ActivitiesServices activitiesServices) {
        this.activitiesServices = activitiesServices;
    }

    @PostMapping("/{eventId}/activities")
    public ResponseEntity<ActivityDTO> createActivity(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateActivityRequest request, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.status(HttpStatus.CREATED).body(activitiesServices.createActivity(UUID.fromString(jwt.getSubject()), eventId, request));
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<MessageResponse> deleteActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        activitiesServices.deleteActivity(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.ok(new MessageResponse("Atividade removida com sucesso."));
    }
}
