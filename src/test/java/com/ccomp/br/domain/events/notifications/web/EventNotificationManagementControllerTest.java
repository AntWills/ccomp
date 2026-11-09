package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.application.EventNotificationService;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;
import com.ccomp.br.domain.events.notifications.persistence.EventNotification;
import com.ccomp.br.shared.utils.CursorPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventNotificationManagementControllerTest {
    @Mock EventNotificationService notificationService;
    @Mock EventNotificationQueryService queryService;
    @InjectMocks EventNotificationManagementController controller;

    private final Long eventId = 42L;
    private final UUID senderId = UUID.randomUUID();

    @Test
    void sendsNotificationToEventSubscribersAndReturnsAccepted() {
        var request = new CreateEventNotificationRequest("Mudança", "O evento mudou de local.");
        EventNotification saved = notification(NotificationAudience.EVENT);
        when(notificationService.sendToEventSubscribers(eventId, request, senderId)).thenReturn(saved);

        var response = controller.sendToEvent(eventId, jwt(senderId), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(new EventNotificationCreatedResponse(
                91L, NotificationStatus.QUEUED, 0));
        verify(notificationService).sendToEventSubscribers(eventId, request, senderId);
    }

    @Test
    void sendsActivityAndSelectedNotificationsUsingAuthenticatedSender() {
        var request = new CreateEventNotificationRequest("Sala", "A sala foi alterada.");
        EventNotification activityNotification = notification(NotificationAudience.ACTIVITY);
        when(notificationService.sendToActivitySubscribers(eq(eventId), eq(7L), eq(request), eq(senderId)))
                .thenReturn(activityNotification);
        var activityResponse = controller.sendToActivity(eventId, 7L, jwt(senderId), request);
        assertThat(activityResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        var selectedRequest = new SendSelectedNotificationRequest(7L, List.of(UUID.randomUUID()), request);
        EventNotification selectedNotification = notification(NotificationAudience.SELECTED);
        when(notificationService.sendToSelectedSubscribers(eventId, selectedRequest, senderId))
                .thenReturn(selectedNotification);
        var selectedResponse = controller.sendToSelected(eventId, jwt(senderId), selectedRequest);
        assertThat(selectedResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(notificationService).sendToActivitySubscribers(eventId, 7L, request, senderId);
        verify(notificationService).sendToSelectedSubscribers(eventId, selectedRequest, senderId);
    }

    @Test
    void listsAndMonitorsSentNotifications() {
        var page = new CursorPage<EventNotificationListItem>(List.of(), null, null);
        var status = new EventNotificationDeliveryStatus(91L, NotificationStatus.COMPLETED,
                5, 0, 5, 0, 0, null);
        when(queryService.listSent(eventId, senderId, "cursor", 15)).thenReturn(page);
        when(queryService.deliveryStatus(eventId, 91L, senderId)).thenReturn(status);

        assertThat(controller.listSent(eventId, jwt(senderId), "cursor", 15).getBody()).isSameAs(page);
        assertThat(controller.deliveryStatus(eventId, 91L, jwt(senderId)).getBody()).isSameAs(status);
        verify(queryService).listSent(eventId, senderId, "cursor", 15);
        verify(queryService).deliveryStatus(eventId, 91L, senderId);
    }

    @Test
    void exposesRetryOperations() {
        assertThat(controller.retryFailed(eventId, 91L, jwt(senderId)).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(controller.retryDispatch(eventId, 91L, jwt(senderId)).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        verify(notificationService).retryFailed(eventId, 91L, senderId);
        verify(notificationService).retryDispatch(eventId, 91L, senderId);
    }

    private EventNotification notification(NotificationAudience audience) {
        EventNotification notification = mock(EventNotification.class);
        when(notification.getId()).thenReturn(91L);
        when(notification.getStatus()).thenReturn(NotificationStatus.QUEUED);
        when(notification.getRecipientCount()).thenReturn(0);
        return notification;
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("test-token").header("alg", "none").subject(userId.toString()).build();
    }
}
