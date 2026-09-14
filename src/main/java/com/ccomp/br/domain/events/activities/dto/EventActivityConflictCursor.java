package com.ccomp.br.domain.events.activities.dto;

import java.time.LocalDateTime;

public record EventActivityConflictCursor(
        Long id,
        LocalDateTime startDate
) {
}
