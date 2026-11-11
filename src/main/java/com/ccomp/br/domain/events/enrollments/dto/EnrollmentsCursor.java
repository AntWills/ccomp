package com.ccomp.br.domain.events.enrollments.dto;

import java.time.LocalDateTime;

public record EnrollmentsCursor(
        Double relevance,
        LocalDateTime createdAt,
        Long id
) {
    public EnrollmentsCursor(LocalDateTime createdAt, Long id) {
        this(null, createdAt, id);
    }
}
