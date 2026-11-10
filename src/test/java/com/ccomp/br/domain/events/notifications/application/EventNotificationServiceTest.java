package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.domain.events.notifications.application.async.EventNotificationPublisher;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {
    private static final Long EVENT_ID = 42L;
    private static final Long ACTIVITY_ID = 7L;
    private static final Long NOTIFICATION_ID = 91L;
    private static final UUID SENDER_ID = UUID.randomUUID();

    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;
    @Mock EventActivityRepository activityRepository;
    @Mock EventNotificationRepository notificationRepository;
    @Mock EventNotificationRecipientRepository recipientRepository;
    @Mock EventNotificationDslRepository dslRepository;
    @Mock EventNotificationPublisher publisher;
    @Mock EventNotificationCacheInvalidator cacheInvalidator;

    private EventNotificationService service;
    private EventDTO event;

    @BeforeEach
    void setUp() {
        service = new EventNotificationService(eventCache, accessPolicy, activityRepository, notificationRepository,
                recipientRepository, dslRepository, publisher, cacheInvalidator,
                Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
        event = mock(EventDTO.class);
        lenient().when(event.title()).thenReturn("DevConf");
        lenient().when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        lenient().when(accessPolicy.canEdit(event, SENDER_ID)).thenReturn(true);
        lenient().when(notificationRepository.save(any(EventNotification.class))).thenAnswer(invocation -> {
            EventNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
            return notification;
        });
    }

    @Test
    void createsEventNotificationAndSchedulesAsyncDispatch() {
        var request = new CreateEventNotificationRequest("Mudança de local", "A palestra mudou de sala.");

        EventNotification result = service.sendToEventSubscribers(EVENT_ID, request, SENDER_ID);

        assertThat(result.getId()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getEventTitle()).isEqualTo("DevConf");
        assertThat(result.getSenderUserId()).isEqualTo(SENDER_ID);
        assertThat(result.getAudience()).isEqualTo(NotificationAudience.EVENT);
        assertThat(result.getStatus().name()).isEqualTo("QUEUED");
        verify(publisher).dispatch(NOTIFICATION_ID);
    }

    @Test
    void activityNotificationValidatesEventOwnership() {
        EventActivity activity = mock(EventActivity.class);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(EVENT_ID);

        EventNotification result = service.sendToActivitySubscribers(EVENT_ID, ACTIVITY_ID,
                new CreateEventNotificationRequest("Horário", "Mudança de horário."), SENDER_ID);

        assertThat(result.getAudience()).isEqualTo(NotificationAudience.ACTIVITY);
        assertThat(result.getActivityId()).isEqualTo(ACTIVITY_ID);
        verify(publisher).dispatch(NOTIFICATION_ID);
    }

    @Test
    void selectedRecipientsAreDeduplicatedValidatedAndQueuedInBatches() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var candidates = List.of(candidate(first, "Ana"), candidate(second, "Bruno"));
        when(dslRepository.findSelectedRecipients(EVENT_ID, null, List.of(first, second)))
                .thenReturn(candidates);
        when(recipientRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<EventNotificationRecipient> recipients = invocation.getArgument(0);
            for (int index = 0; index < recipients.size(); index++)
                ReflectionTestUtils.setField(recipients.get(index), "id", 301L + index);
            return recipients;
        });
        var request = new SendSelectedNotificationRequest(null, List.of(first, second, first),
                new CreateEventNotificationRequest("Aviso", "Mensagem."));

        EventNotification result = service.sendToSelectedSubscribers(EVENT_ID, request, SENDER_ID);

        assertThat(result.getAudience()).isEqualTo(NotificationAudience.SELECTED);
        assertThat(result.getRecipientCount()).isEqualTo(2);
        assertThat(result.getDispatchCompletedAt()).isNotNull();
        verify(dslRepository).findSelectedRecipients(EVENT_ID, null, List.of(first, second));
        verify(publisher).deliver(NOTIFICATION_ID, List.of(301L, 302L));
        verify(publisher, never()).dispatch(any());
    }

    @Test
    void selectedRecipientOutsideScopeIsRejectedBeforePersistence() {
        UUID userId = UUID.randomUUID();
        when(dslRepository.findSelectedRecipients(EVENT_ID, null, List.of(userId))).thenReturn(List.of());
        var request = new SendSelectedNotificationRequest(null, List.of(userId),
                new CreateEventNotificationRequest("Aviso", "Mensagem."));

        assertThatThrownBy(() -> service.sendToSelectedSubscribers(EVENT_ID, request, SENDER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Um ou mais usuários selecionados não estão inscritos no escopo informado.");
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void sendingRequiresEventEditPermission() {
        when(accessPolicy.canEdit(event, SENDER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.sendToEventSubscribers(EVENT_ID,
                new CreateEventNotificationRequest("Aviso", "Mensagem."), SENDER_ID))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    private NotificationRecipientCandidate candidate(UUID userId, String name) {
        return new NotificationRecipientCandidate(userId, name, name.toLowerCase() + "@example.org",
                java.time.LocalDateTime.parse("2026-11-09T10:00:00"), userId.hashCode() & 0x7fffffffL);
    }
}
