package com.ccomp.br.domain.events.enums.activities;

import java.time.LocalDateTime;

public record EnrollmentActivityCursor(
    Long id,
    LocalDateTime createdAt
) {
}
