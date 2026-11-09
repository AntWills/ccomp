package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventNotificationQueryService {
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final EventNotificationRepository notificationRepository;
    private final EventNotificationRecipientRepository recipientRepository;
    private final EventNotificationDslRepository dslRepository;
    private final EventNotificationOutboxRepository outboxRepository;
    private final EventNotificationRecipientDslRepository recipientDslRepository;
    private final Clock clock;

    public EventNotificationQueryService(EventCache eventCache, EventAccessPolicy accessPolicy,
            EventNotificationRepository notificationRepository,
            EventNotificationRecipientRepository recipientRepository,
            EventNotificationDslRepository dslRepository, EventNotificationOutboxRepository outboxRepository,
            EventNotificationRecipientDslRepository recipientDslRepository, Clock clock) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.dslRepository = dslRepository;
        this.outboxRepository = outboxRepository;
        this.recipientDslRepository = recipientDslRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventNotificationListItem> listSent(Long eventId, UUID userId, String cursor, int pageSize) {
        assertCanEdit(eventId, userId);
        int size = Math.max(1, Math.min(pageSize, 50));
        EventNotificationCursor decoded = CursorUtils.decode(cursor, EventNotificationCursor.class);
        List<EventNotificationListItem> results = dslRepository.findSentByEvent(eventId, decoded, size + 1);
        return CursorUtils.buildPage(results, size, item -> new EventNotificationCursor(item.createdAt(), item.id()));
    }

    @Transactional(readOnly = true)
    public EventNotificationDeliveryStatus deliveryStatus(Long eventId, Long notificationId, UUID userId) {
        assertCanEdit(eventId, userId);
        EventNotification notification = notificationRepository.findByIdAndEventId(notificationId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada neste evento."));
        var pendingOutbox = outboxRepository.findFirstByNotificationIdAndPublishedAtIsNullOrderByIdDesc(notificationId);
        return new EventNotificationDeliveryStatus(notificationId, notification.getStatus(),
                recipientRepository.countByNotificationId(notificationId),
                recipientRepository.countByNotificationIdAndEmailStatus(notificationId, NotificationEmailStatus.PENDING)
                        + recipientRepository.countByNotificationIdAndEmailStatus(notificationId, NotificationEmailStatus.PROCESSING),
                recipientRepository.countByNotificationIdAndEmailStatus(notificationId, NotificationEmailStatus.SENT),
                recipientRepository.countByNotificationIdAndEmailStatus(notificationId, NotificationEmailStatus.FAILED),
                outboxRepository.countByNotificationIdAndPublishedAtIsNull(notificationId),
                pendingOutbox.map(EventNotificationOutbox::getLastError).orElse(null));
    }

    @Transactional(readOnly = true)
    public CursorPage<MyEventNotificationItem> listMine(UUID userId, boolean unreadOnly, String cursor, int pageSize) {
        int size = Math.max(1, Math.min(pageSize, 50));
        EventNotificationCursor decoded = CursorUtils.decode(cursor, EventNotificationCursor.class);
        List<MyEventNotificationItem> results = dslRepository.findForUser(userId, unreadOnly, decoded, size + 1);
        return CursorUtils.buildPage(results, size, item -> new EventNotificationCursor(item.sentAt(), item.id()));
    }

    @Transactional
    public void markRead(UUID userId, Long recipientId) {
        EventNotificationRecipient recipient = recipientRepository.findByIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));
        if (recipient.getReadAt() == null) {
            recipient.setReadAt(LocalDateTime.now(clock));
            recipientRepository.save(recipient);
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return recipientDslRepository.markAllRead(userId, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) { return recipientRepository.countByUserIdAndReadAtIsNull(userId); }

    private void assertCanEdit(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Você não tem permissão para consultar os envios deste evento.");
    }
}
