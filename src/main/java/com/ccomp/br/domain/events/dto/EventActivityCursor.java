package com.ccomp.br.domain.events.dto;

import java.time.LocalDateTime;

public record EventActivityCursor (
        LocalDateTime createdAd,
        Long id
) {
}
