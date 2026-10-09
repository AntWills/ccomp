package com.ccomp.br.domain.events.enrollments.dto;

import com.ccomp.br.shared.dto.UserSummaryDTO;

import java.time.LocalDateTime;

public record UserActivitySummaryDTO(
        UserSummaryDTO user,
        boolean haveCheckIn,
        Long id,
        LocalDateTime createdAt
) {
}
