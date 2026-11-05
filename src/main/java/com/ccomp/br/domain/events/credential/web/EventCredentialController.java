package com.ccomp.br.domain.events.credential.web;

import com.ccomp.br.domain.events.credential.application.EventCredentialService;
import com.ccomp.br.domain.events.credential.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/credentials")
public class EventCredentialController {
    private final EventCredentialService service;

    public EventCredentialController(EventCredentialService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public ResponseEntity<CredentialResponse> scan(@PathVariable Long eventId,
            @Valid @RequestBody CredentialScanRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.scan(eventId, userId(jwt), request));
    }

    @PostMapping("/manual/search")
    public ResponseEntity<List<CredentialSearchItem>> search(@PathVariable Long eventId,
            @Valid @RequestBody CredentialSearchRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.search(eventId, userId(jwt), request));
    }

    @PostMapping("/manual")
    public ResponseEntity<CredentialResponse> manual(@PathVariable Long eventId,
            @Valid @RequestBody ManualCredentialRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.manual(eventId, userId(jwt), request));
    }

    @PostMapping("/{credentialId}/reprint")
    public ResponseEntity<CredentialResponse> reprint(@PathVariable Long eventId,
            @PathVariable Long credentialId, @Valid @RequestBody BadgePrintRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.reprint(eventId, credentialId, userId(jwt), request.station()));
    }

    @PostMapping("/my-token")
    public ResponseEntity<CredentialTokenResponse> myToken(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.myToken(eventId, userId(jwt)));
    }

    @PostMapping("/my-token/rotate")
    public ResponseEntity<CredentialTokenResponse> rotateMyToken(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.rotateMyToken(eventId, userId(jwt)));
    }

    @PostMapping("/enrollments/{enrollmentId}/token/rotate")
    public ResponseEntity<CredentialTokenResponse> rotateParticipantToken(@PathVariable Long eventId,
            @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.rotateParticipantToken(eventId, enrollmentId, userId(jwt)));
    }

    @GetMapping("/summary")
    public ResponseEntity<CredentialSummaryResponse> summary(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.summary(eventId, userId(jwt)));
    }

    @PutMapping("/operators/{userId}")
    public ResponseEntity<Void> addOperator(@PathVariable Long eventId, @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        service.addOperator(eventId, userId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/operators/{operatorUserId}")
    public ResponseEntity<Void> removeOperator(@PathVariable Long eventId, @PathVariable UUID operatorUserId,
            @AuthenticationPrincipal Jwt jwt) {
        service.removeOperator(eventId, operatorUserId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
