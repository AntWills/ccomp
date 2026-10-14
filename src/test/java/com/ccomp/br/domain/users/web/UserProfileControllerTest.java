package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.domain.users.dto.UpdateUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
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
class UserProfileControllerTest {
    @Mock private UserApplication userApplication;
    @InjectMocks private UserProfileController controller;

    @Test
    void getMeReturnsProfileForJwtSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        UserDTO user = mock(UserDTO.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(userApplication.getById(userId)).thenReturn(Optional.of(user));

        var response = controller.getMe(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(user);
    }

    @Test
    void getMeReturnsNotFoundWhenSubjectHasNoProfile() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(userApplication.getById(userId)).thenReturn(Optional.empty());

        assertThat(controller.getMe(jwt).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUsesJwtSubjectAndReturnsUpdatedProfile() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        UpdateUserDTO request = new UpdateUserDTO("Updated name");
        UserDTO user = mock(UserDTO.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(userApplication.update(userId, request)).thenReturn(user);

        var response = controller.update(request, jwt);

        assertThat(response.getBody()).isSameAs(user);
        verify(userApplication).update(userId, request);
    }

    @Test
    void deactivateAccountUsesJwtSubjectAndReturnsConfirmation() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());

        var response = controller.deactivateAccount(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().response()).isEqualTo("Conta desativada com sucesso!");
        verify(userApplication).deactivateOwnAccount(userId);
    }
}
