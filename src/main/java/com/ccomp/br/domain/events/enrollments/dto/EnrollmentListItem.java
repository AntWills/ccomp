package com.ccomp.br.domain.events.enrollments.dto;

import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryDTO;

import java.time.LocalDateTime;

public record EnrollmentListItem (
        UserSummaryDTO user,
        Long id,
        EnumEnrollmentState status,
        LocalDateTime createdAt
) {
}
