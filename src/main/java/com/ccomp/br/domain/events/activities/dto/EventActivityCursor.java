package com.ccomp.br.domain.events.activities.dto;

import java.time.LocalDateTime;

public record EventActivityCursor (
        LocalDateTime createdAd,
        Long id
) {
}
