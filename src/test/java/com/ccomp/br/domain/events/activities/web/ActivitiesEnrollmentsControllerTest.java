package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.shared.application.ActivitiesEnrollmentsServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.domain.events.enrollments.dto.UserActivitySummaryDTO;

import java.util.List;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitiesEnrollmentsControllerTest {
    @Mock private ActivitiesEnrollmentsServices service;
    @InjectMocks private ActivitiesEnrollmentsController controller;

    @Test
    void managerEnrollmentDelegatesActivityEnrollmentAndManager() {
        UUID managerId = UUID.randomUUID();
        when(service.enrollParticipant(managerId, 31L, 72L))
                .thenReturn(new com.ccomp.br.shared.dto.MessageResponse("Pessoa inscrita na atividade com sucesso."));

        var response = controller.enrollParticipant(31L, 72L, jwt(managerId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isEqualTo("Pessoa inscrita na atividade com sucesso.");
        verify(service).enrollParticipant(managerId, 31L, 72L);
    }

    @Test
    void activityEnrollmentListingForwardsSearchTermAndCursorParameters() {
        UUID managerId = UUID.randomUUID();
        when(service.findAllUsersFromActivity(managerId, 31L, "cursor-token", 20, "ana silva"))
                .thenReturn(new CursorPage<UserActivitySummaryDTO>(List.of(), null, null));

        var response = controller.getActivityEnrollments(31L, "cursor-token", 20, "ana silva", jwt(managerId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).findAllUsersFromActivity(managerId, 31L, "cursor-token", 20, "ana silva");
    }

    @Test
    void selfSubscribeAndUnsubscribeDelegateAuthenticatedUserAndActivity() {
        UUID userId = UUID.randomUUID();
        when(service.inscribe(userId, 31L)).thenReturn(new com.ccomp.br.shared.dto.MessageResponse("ok"));
        when(service.unsubscribe(userId, 31L)).thenReturn(new com.ccomp.br.shared.dto.MessageResponse("ok"));

        controller.subscribeToActivity(31L, jwt(userId));
        controller.unsubscribeFromActivity(31L, jwt(userId));

        verify(service).inscribe(userId, 31L);
        verify(service).unsubscribe(userId, 31L);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }
}
