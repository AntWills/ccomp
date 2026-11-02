package com.ccomp.br.domain.events.materials.dto;

import java.time.LocalDateTime;

public record EventMaterialCursor(LocalDateTime createdAt, Long id) {
}
