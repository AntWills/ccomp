package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.activities.application.ActivitiesServices;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.CreateActivityDTO;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitiesControllerTest {
    @Mock
    private ActivitiesServices activitiesServices;

    @InjectMocks
    private ActivitiesController controller;

    @Test
    void getActivitiesDelegatesEventCursorAndOptionalUser() {
        Long eventId = 12L;
        String cursor = "encoded-cursor";
        CursorPage<EventActivityDTO> expected = new CursorPage<>(List.of(), null, null);
        when(activitiesServices.searchByCursor(eventId, cursor, null)).thenReturn(expected);

        var response = controller.getActivities(eventId, cursor, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(activitiesServices).searchByCursor(eventId, cursor, null);
    }

    @Test
    void createActivityReturnsCreatedAndPassesAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId);
        CreateActivityDTO request = new CreateActivityDTO("Palestra", "Descrição");
        ActivityDTO expected = activity();
        when(activitiesServices.createActivity(userId, 12L, request)).thenReturn(expected);

        var response = controller.createActivity(12L, request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void updateActivityDelegatesRequestAndAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId);
        UpdateActivityDTO request = UpdateActivityDTO.builder().title("Título alterado").build();
        ActivityDTO expected = activity();
        when(activitiesServices.updateActivity(userId, 44L, request)).thenReturn(expected);

        var response = controller.updateActivity(44L, request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void deleteActivityDelegatesAndReturnsSuccessMessage() {
        UUID userId = UUID.randomUUID();
        var response = controller.deleteActivity(44L, jwt(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new MessageResponse("Atividade removida com sucesso."));
        verify(activitiesServices).deleteActivity(userId, 44L);
    }

    @Test
    void createActivityRequiresAuthenticatedUser() {
        assertThatThrownBy(() -> controller.createActivity(12L,
                new CreateActivityDTO("Palestra", null), null))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(activitiesServices);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }

    private static ActivityDTO activity() {
        return new ActivityDTO(44L, 12L, "Palestra", null, 0L, null,
                null, null, null, null, null);
    }
}
