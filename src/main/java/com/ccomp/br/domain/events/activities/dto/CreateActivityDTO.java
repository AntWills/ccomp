package com.ccomp.br.domain.events.activities.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateActivityDTO(
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description,

        @PositiveOrZero(message = "A capacidade máxima não pode ser negativa.")
        @Schema(description = "Número máximo de inscrições ativas nesta atividade. Null significa sem limite.", example = "40")
        Integer maxCapacity
) {
    public CreateActivityDTO(String title, String description) {
        this(title, description, null);
    }
}
