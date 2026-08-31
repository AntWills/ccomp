package com.ccomp.br.domain.events.dto.enrollments;

import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryView;

import java.time.LocalDateTime;

public record EnrollmentListItem (
        Long id,
        EnumEnrollmentState status,
        LocalDateTime createdAt,
        UserSummaryView user
) {
}
