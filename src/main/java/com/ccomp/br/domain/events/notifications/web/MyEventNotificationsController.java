package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.dto.MyEventNotificationItem;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/me/notifications")
@Tag(name = "events/notifications (inscrito)",
        description = "Caixa pessoal de notificações do usuário autenticado e controle de leitura.")
public class MyEventNotificationsController {
    private final EventNotificationQueryService queryService;

    public MyEventNotificationsController(EventNotificationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Lista minhas notificações",
            description = "Retorna somente notificações destinadas ao usuário autenticado, ordenadas por data decrescente e paginadas por cursor. Use `unreadOnly=true` para retornar apenas notificações ainda não lidas. `pageSize` tem padrão 20 e máximo efetivo 50.")
    public ResponseEntity<CursorPage<MyEventNotificationItem>> listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Se true, lista apenas notificações ainda não lidas")
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Parameter(description = "Cursor recebido como `nextCursor` da página anterior")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens por página; padrão 20, máximo efetivo 50")
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(queryService.listMine(userId(jwt), unreadOnly, nextCursor, pageSize));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Conta notificações não lidas",
            description = "Retorna a quantidade de notificações não lidas destinadas ao usuário autenticado.")
    public ResponseEntity<Long> unreadCount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(queryService.unreadCount(userId(jwt)));
    }

    @PutMapping("/{recipientId}/read")
    @Operation(summary = "Marca uma notificação como lida",
            description = "Atualiza read_at somente no registro de destinatário do usuário autenticado. O `recipientId` é o ID da associação destinatário-notificação retornado como `id` pela listagem pessoal; outro usuário não pode alterar este registro.")
    public ResponseEntity<Void> markRead(
            @Parameter(description = "ID do destinatário da notificação (campo `id` da listagem pessoal)")
            @PathVariable Long recipientId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        queryService.markRead(userId(jwt), recipientId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Marca todas as notificações como lidas",
            description = "Marca como lidas todas as notificações não lidas do usuário autenticado e retorna a quantidade de registros atualizados.")
    public ResponseEntity<MessageResponse> markAllRead(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        int updated = queryService.markAllRead(userId(jwt));
        return ResponseEntity.ok(new MessageResponse("%d notificações marcadas como lidas.".formatted(updated)));
    }

    private UUID userId(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject).map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
    }
}
