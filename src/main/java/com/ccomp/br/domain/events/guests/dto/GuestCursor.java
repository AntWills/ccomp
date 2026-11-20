package com.ccomp.br.domain.events.guests.dto;

import java.time.LocalDateTime;

public record GuestCursor(
        Double relevance,
        Long id,
        LocalDateTime createdAt
) {
    public GuestCursor(Long id, LocalDateTime createdAt) {
        this(null, id, createdAt);
    }
}
