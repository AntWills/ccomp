package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.core.application.AuthApplication;
import com.ccomp.br.domain.auth.core.web.AuthController;
import com.ccomp.br.domain.auth.core.dto.*;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthApplication authApplication;
    @InjectMocks private AuthController authController;

    @Test
    void signUpReturnsCreatedAndDelegatesRegistration() {
        RegisterUserDTO request = new RegisterUserDTO("Test User", new EmailAddress("test@example.com"), "password123");

        var response = authController.signUp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("message", "Conta criada com sucesso."));
        verify(authApplication).signUp(request);
    }

    @Test
    void signInPassesRequestMetadataToApplication() {
        LoginRequestDTO request = new LoginRequestDTO(new EmailAddress("test@example.com"), "password123");
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(servletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        TokenPair expected = TokenPair.builder().accessToken("access").refreshToken(UUID.randomUUID()).build();
        when(authApplication.signIn(eq(request), any(ClientMetadataDTO.class))).thenReturn(expected);

        assertThat(authController.login(request, servletRequest)).isSameAs(expected);
        verify(authApplication).signIn(eq(request), eq(new ClientMetadataDTO("127.0.0.1", "JUnit")));
    }

    @Test
    void refreshReturnsUnauthorizedWhenRefreshTokenIsRejected() {
        RefreshTokenRequest request = new RefreshTokenRequest(UUID.randomUUID());
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(authApplication.refresh(eq(request), any(ClientMetadataDTO.class))).thenReturn(Optional.empty());

        var response = authController.refresh(request, servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void refreshReturnsTokenPairWhenRefreshSucceeds() {
        RefreshTokenRequest request = new RefreshTokenRequest(UUID.randomUUID());
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        TokenPair expected = TokenPair.builder().accessToken("new-access").refreshToken(UUID.randomUUID()).build();
        when(authApplication.refresh(eq(request), any(ClientMetadataDTO.class))).thenReturn(Optional.of(expected));

        var response = authController.refresh(request, servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void logoutReturnsNoContentAndDelegatesTokenRevocation() {
        RefreshTokenRequest request = new RefreshTokenRequest(UUID.randomUUID());

        assertThat(authController.logout(request).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authApplication).logout(request);
    }
}
