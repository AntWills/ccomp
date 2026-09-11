package com.ccomp.br.domain.events.dto.enrollments;

import com.ccomp.br.shared.dto.UserSummaryDTO;

import java.time.LocalDateTime;

public record UserActivitySummaryDTO(
        UserSummaryDTO user,
        Long id,
        LocalDateTime createdAt
) {
}
