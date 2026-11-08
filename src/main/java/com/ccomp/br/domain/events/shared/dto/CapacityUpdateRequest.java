package com.ccomp.br.domain.events.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Define a capacidade máxima; null remove o limite.")
public record CapacityUpdateRequest(
        @PositiveOrZero(message = "A capacidade máxima não pode ser negativa.")
        Integer maxCapacity
) {
}
