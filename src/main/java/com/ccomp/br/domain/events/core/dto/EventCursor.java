package com.ccomp.br.domain.events.core.dto;

import java.time.LocalDateTime;

public record EventCursor(LocalDateTime startDate, Long id) {
}
