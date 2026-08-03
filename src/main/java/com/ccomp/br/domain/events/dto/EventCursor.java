package com.ccomp.br.domain.events.dto;

import java.time.LocalDateTime;

public record EventCursor(LocalDateTime startDate, Long id) {
}
