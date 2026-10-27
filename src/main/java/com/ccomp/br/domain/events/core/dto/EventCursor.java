package com.ccomp.br.domain.events.core.dto;

import java.time.LocalDateTime;

public record EventCursor(
        Double relevance,
        LocalDateTime startDate,
        Long id) {
    public EventCursor(LocalDateTime startDate, Long id) {
        this(null, startDate, id);
    }
}
