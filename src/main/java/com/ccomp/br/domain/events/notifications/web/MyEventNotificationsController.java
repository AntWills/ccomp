package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.dto.MyEventNotificationItem;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/me/notifications")
public class MyEventNotificationsController {
    private final EventNotificationQueryService queryService;

    public MyEventNotificationsController(EventNotificationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<MyEventNotificationItem>> listMine(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(queryService.listMine(userId(jwt), unreadOnly, nextCursor, pageSize));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(queryService.unreadCount(userId(jwt)));
    }

    @PutMapping("/{recipientId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long recipientId, @AuthenticationPrincipal Jwt jwt) {
        queryService.markRead(userId(jwt), recipientId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        int updated = queryService.markAllRead(userId(jwt));
        return ResponseEntity.ok(new MessageResponse("%d notificações marcadas como lidas.".formatted(updated)));
    }

    private UUID userId(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject).map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
    }
}
