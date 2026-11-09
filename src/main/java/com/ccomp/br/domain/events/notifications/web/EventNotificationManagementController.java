package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.application.EventNotificationService;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import jakarta.validation.Valid;
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
@RequestMapping("/api/events/{eventId}/notifications")
@Tag(name = "events/notifications (gestão)",
        description = "Envio manual de notificações aos inscritos de eventos e atividades, histórico e acompanhamento da entrega.")
public class EventNotificationManagementController {
    private final EventNotificationService notificationService;
    private final EventNotificationQueryService queryService;

    public EventNotificationManagementController(EventNotificationService notificationService,
            EventNotificationQueryService queryService) {
        this.notificationService = notificationService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Notifica os inscritos do evento",
            description = "Cria uma notificação na plataforma para cada inscrição ativa (CONFIRMED ou CHECKED_IN) do evento e agenda o envio de email assíncrono. A requisição retorna 202 com o identificador do envio; acompanhe o processamento na rota de entrega. Requer permissão de edição do evento. O corpo aceita título de até 255 caracteres e mensagem de até 10.000 caracteres.")
    public ResponseEntity<EventNotificationCreatedResponse> sendToEvent(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEventNotificationRequest request) {
        var notification = notificationService.sendToEventSubscribers(eventId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @PostMapping("/activities/{activityId}")
    @Operation(summary = "Notifica os inscritos de uma atividade",
            description = "Envia somente aos inscritos ativos vinculados à atividade informada por EnrollmentActivity; inscritos apenas no evento não são incluídos. A atividade precisa pertencer ao evento. Cria notificações na plataforma e agenda emails de forma assíncrona. Requer permissão de edição do evento.")
    public ResponseEntity<EventNotificationCreatedResponse> sendToActivity(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(description = "ID da atividade; a audiência é limitada aos inscritos ativos nela")
            @PathVariable Long activityId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEventNotificationRequest request) {
        var notification = notificationService.sendToActivitySubscribers(eventId, activityId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @PostMapping("/selected")
    @Operation(summary = "Notifica inscritos selecionados",
            description = "Envia para os IDs de usuário selecionados, validando que todos estejam inscritos no escopo. `activityId` opcional restringe o escopo à atividade; se omitido, valida as inscrições do evento. IDs repetidos são deduplicados. O limite é 500 usuários por chamada. Requer permissão de edição do evento.")
    public ResponseEntity<EventNotificationCreatedResponse> sendToSelected(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendSelectedNotificationRequest request) {
        var notification = notificationService.sendToSelectedSubscribers(eventId, request, userId(jwt));
        return ResponseEntity.accepted().body(new EventNotificationCreatedResponse(notification.getId(),
                notification.getStatus(), notification.getRecipientCount()));
    }

    @GetMapping
    @Operation(summary = "Lista os envios do evento",
            description = "Retorna histórico ordenado por criação decrescente com cursor (`nextCursor`) e limite de 1 a 50 itens. Cada item identifica remetente, escopo, conteúdo e estado de processamento. Inclui envios em fila ou em andamento. Requer permissão de edição do evento.")
    public ResponseEntity<CursorPage<EventNotificationListItem>> listSent(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Cursor recebido como `nextCursor` da página anterior")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens por página; padrão 20, máximo efetivo 50")
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(queryService.listSent(eventId, userId(jwt), nextCursor, pageSize));
    }

    @GetMapping("/{notificationId}/delivery")
    @Operation(summary = "Consulta o progresso e resultado do envio",
            description = "Retorna totais de destinatários, emails pendentes/enviados/falhos, mensagens ainda pendentes na outbox e o último erro de publicação ao RabbitMQ. Status possíveis: QUEUED, PROCESSING, COMPLETED e COMPLETED_WITH_ERRORS. Requer permissão de edição do evento.")
    public ResponseEntity<EventNotificationDeliveryStatus> deliveryStatus(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(description = "ID do envio de notificação") @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(queryService.deliveryStatus(eventId, notificationId, userId(jwt)));
    }

    @PostMapping("/{notificationId}/retry-failed")
    @Operation(summary = "Reenfileira emails que falharam",
            description = "Reseta destinatários com email_status FAILED para PENDING e publica novos lotes pela outbox. A entrega na plataforma já existe e não é duplicada. Requer permissão de edição do evento.")
    public ResponseEntity<MessageResponse> retryFailed(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(description = "ID do envio") @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        notificationService.retryFailed(eventId, notificationId, userId(jwt));
        return ResponseEntity.accepted().body(new MessageResponse("Destinatários com falha foram reenfileirados."));
    }

    @PostMapping("/{notificationId}/retry-dispatch")
    @Operation(summary = "Retoma a distribuição de destinatários",
            description = "Reenfileira a distribuição por cursor quando o envio ainda não tem dispatch concluído. Destinatários já persistidos são reconhecidos e não duplicados. Requer permissão de edição do evento.")
    public ResponseEntity<MessageResponse> retryDispatch(
            @Parameter(description = "ID do evento") @PathVariable Long eventId,
            @Parameter(description = "ID do envio") @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        notificationService.retryDispatch(eventId, notificationId, userId(jwt));
        return ResponseEntity.accepted().body(new MessageResponse("Distribuição da notificação reenfileirada."));
    }

    private UUID userId(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject).map(UUID::fromString)
                .orElseThrow(() -> new UserNotFoundException("O usuário precisa estar autenticado."));
    }
}
