package com.ccomp.br.domain.events.enrollments.dto;

import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;

import java.time.LocalDateTime;

public record UserActivitySummaryDTO(
        UserSummaryDTO user,
        boolean haveCheckIn,
        boolean canceled,
        EnumEnrollmentState eventEnrollmentStatus,
        Long id,
        LocalDateTime createdAt
) {
    public UserActivitySummaryDTO(UserSummaryDTO user, boolean haveCheckIn, Long id, LocalDateTime createdAt) {
        this(user, haveCheckIn, false, null, id, createdAt);
    }

    public UserActivitySummaryDTO(UserSummaryDTO user, boolean haveCheckIn, boolean canceled,
                                  Long id, LocalDateTime createdAt) {
        this(user, haveCheckIn, canceled, null, id, createdAt);
    }
}
