package com.ccomp.br.domain.events.activities.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckInDTO (
        Long id,
        Long activityId,
        UUID code,
        LocalDateTime createdAt
) {
    public boolean checkCode(UUID other) {
        return other.equals(code);
    }
}
