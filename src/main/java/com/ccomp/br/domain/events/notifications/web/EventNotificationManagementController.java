package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.application.EventNotificationService;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/notifications")
public class EventNotificationManagementController {
    private final EventNotificationService notificationService;
    private final EventNotificationQueryService queryService;

    public EventNotificationManagementController(EventNotificationService notificationService,
            EventNotificationQueryService queryService) {
        this.notificationService = notificationService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<EventNotificationCreatedResponse> sendToEvent(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateEventNotificationRequest request) {
        var notification = notificationService.sendToEventSubscribers(eventId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @PostMapping("/activities/{activityId}")
    public ResponseEntity<EventNotificationCreatedResponse> sendToActivity(@PathVariable Long eventId,
            @PathVariable Long activityId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEventNotificationRequest request) {
        var notification = notificationService.sendToActivitySubscribers(eventId, activityId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @PostMapping("/selected")
    public ResponseEntity<EventNotificationCreatedResponse> sendToSelected(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendSelectedNotificationRequest request) {
        var notification = notificationService.sendToSelectedSubscribers(eventId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @GetMapping
    public ResponseEntity<CursorPage<EventNotificationListItem>> listSent(@PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(queryService.listSent(eventId, userId(jwt), nextCursor, pageSize));
    }

    @GetMapping("/{notificationId}/delivery")
    public ResponseEntity<EventNotificationDeliveryStatus> deliveryStatus(@PathVariable Long eventId,
            @PathVariable Long notificationId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(queryService.deliveryStatus(eventId, notificationId, userId(jwt)));
    }

    @PostMapping("/{notificationId}/retry-failed")
    public ResponseEntity<MessageResponse> retryFailed(@PathVariable Long eventId,
            @PathVariable Long notificationId, @AuthenticationPrincipal Jwt jwt) {
        notificationService.retryFailed(eventId, notificationId, userId(jwt));
        return ResponseEntity.accepted().body(new MessageResponse("Destinatários com falha foram reenfileirados."));
    }

    @PostMapping("/{notificationId}/retry-dispatch")
    public ResponseEntity<MessageResponse> retryDispatch(@PathVariable Long eventId,
            @PathVariable Long notificationId, @AuthenticationPrincipal Jwt jwt) {
        notificationService.retryDispatch(eventId, notificationId, userId(jwt));
        return ResponseEntity.accepted().body(new MessageResponse("Distribuição da notificação reenfileirada."));
    }

    private UUID userId(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject).map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
    }
}
