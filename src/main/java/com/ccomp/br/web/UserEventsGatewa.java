package com.ccomp.br.web;

import com.ccomp.br.domain.events.management.EventsManagement;
import com.ccomp.br.shared.dto.EventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserEventsGatewa {
    private final EventsManagement eventsManagement;

    public UserEventsGatewa(EventsManagement eventsManagement) {
        this.eventsManagement = eventsManagement;
    }

    @GetMapping("/registered")
    public ResponseEntity<List<EventResponse>> getRegistered(@AuthenticationPrincipal Jwt jwt){
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsManagement.findAllByOwnerId(ownerId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<EventResponse>> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID participantId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(eventsManagement.findAllSubscriptions(participantId));
    }
}
