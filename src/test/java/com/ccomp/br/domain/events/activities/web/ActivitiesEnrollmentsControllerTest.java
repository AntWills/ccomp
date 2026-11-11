package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.shared.application.ActivitiesEnrollmentsServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }
}
