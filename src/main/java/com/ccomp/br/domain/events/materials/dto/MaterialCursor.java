package com.ccomp.br.domain.events.materials.dto;

import java.time.LocalDateTime;

public record MaterialCursor(LocalDateTime createdAt, Long id) {
}
