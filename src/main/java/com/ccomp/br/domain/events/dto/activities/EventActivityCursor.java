package com.ccomp.br.domain.events.dto.activities;

import java.time.LocalDateTime;

public record EventActivityCursor (
        LocalDateTime createdAd,
        Long id
) {
}
