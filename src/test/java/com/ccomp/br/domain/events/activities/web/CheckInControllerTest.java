package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.activities.application.CheckInService;
import com.ccomp.br.domain.events.activities.dto.CheckInRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInControllerTest {
    @Mock
    private CheckInService checkInService;

    @InjectMocks
    private CheckInController controller;

    @Test
    void getQRCodeReturnsPngFromService() throws java.io.IOException {
        UUID userId = UUID.randomUUID();
        byte[] image = {1, 2, 3};
        when(checkInService.generateCode(31L, userId)).thenReturn(image);

        var response = controller.getQRCode(31L, jwt(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).isSameAs(image);
    }

    @Test
    void checkInDelegatesPresenceCodeAndReturnsConfirmation() {
        UUID userId = UUID.randomUUID();
        UUID code = UUID.randomUUID();

        var response = controller.checkIn(31L, new CheckInRequest(code), jwt(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isEqualTo("Check-in realizado com sucesso.");
        verify(checkInService).checkIn(31L, userId, code);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }
}
