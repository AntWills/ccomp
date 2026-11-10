package com.ccomp.br.domain.events.notifications.web;

import com.ccomp.br.domain.events.notifications.application.EventNotificationQueryService;
import com.ccomp.br.domain.events.notifications.dto.MyEventNotificationItem;
import com.ccomp.br.shared.utils.CursorPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyEventNotificationsControllerTest {
    @Mock EventNotificationQueryService queryService;
    @InjectMocks MyEventNotificationsController controller;

    @Test
    void listsUnreadNotificationsAndReturnsUnreadCount() {
        UUID userId = UUID.randomUUID();
        var page = new CursorPage<MyEventNotificationItem>(List.of(), null, null);
        when(queryService.listMine(userId, true, "cursor", 12)).thenReturn(page);
        when(queryService.unreadCount(userId)).thenReturn(3L);

        assertThat(controller.listMine(jwt(userId), true, "cursor", 12).getBody()).isSameAs(page);
        assertThat(controller.unreadCount(jwt(userId)).getBody()).isEqualTo(3L);
        verify(queryService).listMine(userId, true, "cursor", 12);
        verify(queryService).unreadCount(userId);
    }

    @Test
    void supportsMarkingOneAndAllNotificationsAsRead() {
        UUID userId = UUID.randomUUID();
        when(queryService.markAllRead(userId)).thenReturn(4);

        assertThat(controller.markRead(17L, jwt(userId)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.markAllRead(jwt(userId)).getBody().response())
                .isEqualTo("4 notificações marcadas como lidas.");
        verify(queryService).markRead(userId, 17L);
        verify(queryService).markAllRead(userId);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("test-token").header("alg", "none").subject(userId.toString()).build();
    }
}
