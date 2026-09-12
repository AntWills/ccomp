package com.ccomp.br.domain.events.enrollments.dto;

import java.time.LocalDateTime;

public record EnrollmentActivityCursor(
    Long id,
    LocalDateTime createdAt
) {
}
