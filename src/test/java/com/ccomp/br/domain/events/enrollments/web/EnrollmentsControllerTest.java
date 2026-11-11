package com.ccomp.br.domain.events.enrollments.web;

import com.ccomp.br.domain.events.enrollments.application.EnrollmentsServices;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
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
class EnrollmentsControllerTest {
    @Mock private EnrollmentsServices service;
    @InjectMocks private EnrollmentsController controller;

    @Test
    void forwardsSearchTermAndStatusToService() {
        UUID manager = UUID.randomUUID();
        var page = new CursorPage<EnrollmentListItem>(List.of(), null, null);
        when(service.searchEnrollments(25L, manager, null, 15,
                EnumEnrollmentState.CONFIRMED, "Ana Silva")).thenReturn(page);

        var response = controller.searchEnrollments(25L, null, 15,
                EnumEnrollmentState.CONFIRMED, "Ana Silva", jwt(manager));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
        verify(service).searchEnrollments(25L, manager, null, 15,
                EnumEnrollmentState.CONFIRMED, "Ana Silva");
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }
}
