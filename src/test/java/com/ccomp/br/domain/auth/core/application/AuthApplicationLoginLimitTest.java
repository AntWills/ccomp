package com.ccomp.br.domain.auth.core.application;

import com.ccomp.br.domain.auth.core.dto.ClientMetadataDTO;
import com.ccomp.br.domain.auth.core.dto.LoginRequestDTO;
import com.ccomp.br.domain.auth.core.dto.TokenPair;
import com.ccomp.br.domain.auth.core.application.AuthApplication;
import com.ccomp.br.domain.auth.jwt.application.JwtService;
import com.ccomp.br.domain.auth.passwordreset.application.PasswordResetService;
import com.ccomp.br.domain.auth.security.UserDetailsImpl;
import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationLoginLimitTest {
    @Mock private UserManagement userManagement;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordResetService passwordResetService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RolesServices rolesServices;
    @Mock private LoginAttemptLimiter loginAttemptLimiter;

    private AuthApplication authApplication;
    private final String email = "person@example.com";
    private final ClientMetadataDTO metadata = new ClientMetadataDTO("127.0.0.1", "JUnit");

    @BeforeEach
    void setUp() {
        authApplication = new AuthApplication(userManagement, jwtService, authenticationManager,
                passwordResetService, rabbitTemplate, rolesServices, loginAttemptLimiter);
    }

    @Test
    void failedAuthenticationIncrementsCounterForSubmittedEmail() {
        var request = new LoginRequestDTO(new EmailAddress(email), "bad-password");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("invalid credentials"));

        assertThatThrownBy(() -> authApplication.signIn(request, metadata))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptLimiter).ensureAllowed(email);
        verify(loginAttemptLimiter).recordFailure(email);
        verify(loginAttemptLimiter, never()).clear(anyString());
    }

    @Test
    void blockedEmailIsRejectedBeforePasswordAuthentication() {
        var request = new LoginRequestDTO(new EmailAddress(email), "any-password");
        doThrow(new RateLimitExceededException(30)).when(loginAttemptLimiter).ensureAllowed(email);

        assertThatThrownBy(() -> authApplication.signIn(request, metadata))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(authenticationManager);
        verify(loginAttemptLimiter, never()).recordFailure(anyString());
    }

    @Test
    void successfulAuthenticationClearsCounterAndReturnsTokens() {
        UUID userId = UUID.randomUUID();
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.getId()).thenReturn(userId);
        doReturn(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(userDetails).getAuthorities();
        when(jwtService.generateAccessToken(userId, List.of("ROLE_USER"))).thenReturn("access");
        UUID refreshToken = UUID.randomUUID();
        when(jwtService.createRefreshToken(userId, metadata)).thenReturn(refreshToken);

        TokenPair result = authApplication.signIn(new LoginRequestDTO(new EmailAddress(email), "good-password"), metadata);

        assertThat(result).isEqualTo(new TokenPair("access", refreshToken));
        verify(loginAttemptLimiter).clear(email);
        verify(loginAttemptLimiter, never()).recordFailure(anyString());
    }
}
