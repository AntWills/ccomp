package com.ccomp.br.domain.events.guests.dto;

import java.time.LocalDateTime;

public record GuestCursor(
        Long id,
        LocalDateTime createdAt
) {
}
