package com.ccomp.br.domain.events.enrollments.dto;

import java.time.LocalDateTime;

public record EnrollmentsCursor(
        LocalDateTime createdAt,
        Long id
) {
}
