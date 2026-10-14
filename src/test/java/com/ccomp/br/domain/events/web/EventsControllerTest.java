package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.web.EventsController;
import com.ccomp.br.shared.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventsControllerTest {
    @Mock private EventsServices eventsServices;
    @InjectMocks private EventsController controller;

    @Test
    void getByIdReturnsEventForAuthenticatedUser() {
        Long eventId = 9L;
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        EventDTO event = mock(EventDTO.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(eventsServices.getById(eventId, userId)).thenReturn(Optional.of(event));

        var response = controller.getById(eventId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(event);
    }

    @Test
    void getByIdReturnsNotFoundForUnknownEvent() {
        when(eventsServices.getById(9L, null)).thenReturn(Optional.empty());

        assertThat(controller.getById(9L, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBySlugReturnsNotFoundWhenServiceHasNoPublishedEvent() {
        when(eventsServices.getBySlug("draft-event")).thenReturn(Optional.empty());

        assertThat(controller.getBySlug("draft-event").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatusDelegatesStatusAndAuthenticatedSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        MessageResponse expected = new MessageResponse("Status do evento alterado para: PUBLISHED");
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(eventsServices.updateEventStatus(9L, EnumEventStatus.PUBLISHED, userId)).thenReturn(expected);

        var response = controller.updateEventStatus(9L, EnumEventStatus.PUBLISHED, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }
}
