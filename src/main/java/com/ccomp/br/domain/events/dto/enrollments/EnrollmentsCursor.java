package com.ccomp.br.domain.events.dto.enrollments;

import java.time.LocalDateTime;

public record EnrollmentsCursor(
        LocalDateTime createdAt,
        Long id
) {
}
