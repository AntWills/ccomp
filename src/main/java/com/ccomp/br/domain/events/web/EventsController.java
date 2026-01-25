package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.application.EventsApplication;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.shared.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return eventsApplication.getById(eventId, ownerId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
