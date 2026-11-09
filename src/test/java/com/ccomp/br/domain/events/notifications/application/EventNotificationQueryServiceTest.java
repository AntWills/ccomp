package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventNotificationQueryServiceTest {
    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;
    @Mock EventNotificationRepository notificationRepository;
    @Mock EventNotificationRecipientRepository recipientRepository;
    @Mock EventNotificationDslRepository dslRepository;
    @Mock EventNotificationOutboxRepository outboxRepository;
    @Mock EventNotificationRecipientDslRepository recipientDslRepository;
    @Mock EventNotificationCacheInvalidator cacheInvalidator;

    private EventNotificationQueryService service;
    private final UUID userId = UUID.randomUUID();
    private EventDTO event;

    @BeforeEach
    void setUp() {
        service = new EventNotificationQueryService(eventCache, accessPolicy, notificationRepository,
                recipientRepository, dslRepository, outboxRepository, recipientDslRepository,
                cacheInvalidator,
                Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
        event = mock(EventDTO.class);
        lenient().when(eventCache.findById(42L)).thenReturn(Optional.of(event));
        lenient().when(accessPolicy.canEdit(event, userId)).thenReturn(true);
    }

    @Test
    void marksOnlyTheAuthenticatedUsersNotificationAsRead() {
        EventNotificationRecipient recipient = EventNotificationRecipient.builder().id(9L).userId(userId).build();
        when(recipientRepository.findByIdAndUserId(9L, userId)).thenReturn(Optional.of(recipient));

        service.markRead(userId, 9L);

        assertThat(recipient.getReadAt()).isEqualTo(java.time.LocalDateTime.parse("2026-11-09T10:00:00"));
        verify(recipientRepository).save(recipient);
    }

    @Test
    void delegatesMarkAllReadToDslAndReturnsUnreadCount() {
        when(recipientDslRepository.markAllRead(eq(userId), any())).thenReturn(3);
        when(recipientRepository.countByUserIdAndReadAtIsNull(userId)).thenReturn(2L);

        assertThat(service.markAllRead(userId)).isEqualTo(3);
        assertThat(service.unreadCount(userId)).isEqualTo(2L);
        verify(recipientDslRepository).markAllRead(eq(userId), any());
    }

    @Test
    void deliveryStatusReportsEmailAndOutboxProgress() {
        EventNotification notification = EventNotification.builder().id(7L).eventId(42L)
                .status(NotificationStatus.PROCESSING).build();
        when(notificationRepository.findByIdAndEventId(7L, 42L)).thenReturn(Optional.of(notification));
        when(recipientRepository.countByNotificationId(7L)).thenReturn(10L);
        when(recipientRepository.countByNotificationIdAndEmailStatus(7L, NotificationEmailStatus.PENDING)).thenReturn(2L);
        when(recipientRepository.countByNotificationIdAndEmailStatus(7L, NotificationEmailStatus.PROCESSING)).thenReturn(1L);
        when(recipientRepository.countByNotificationIdAndEmailStatus(7L, NotificationEmailStatus.SENT)).thenReturn(6L);
        when(recipientRepository.countByNotificationIdAndEmailStatus(7L, NotificationEmailStatus.FAILED)).thenReturn(1L);
        EventNotificationOutbox outbox = EventNotificationOutbox.builder().lastError("Broker unavailable").build();
        when(outboxRepository.countByNotificationIdAndPublishedAtIsNull(7L)).thenReturn(1L);
        when(outboxRepository.findFirstByNotificationIdAndPublishedAtIsNullOrderByIdDesc(7L))
                .thenReturn(Optional.of(outbox));

        var status = service.deliveryStatus(42L, 7L, userId);

        assertThat(status.recipientCount()).isEqualTo(10);
        assertThat(status.emailPending()).isEqualTo(3);
        assertThat(status.emailSent()).isEqualTo(6);
        assertThat(status.emailFailed()).isEqualTo(1);
        assertThat(status.outboxPending()).isEqualTo(1);
        assertThat(status.lastOutboxError()).isEqualTo("Broker unavailable");
    }
}
