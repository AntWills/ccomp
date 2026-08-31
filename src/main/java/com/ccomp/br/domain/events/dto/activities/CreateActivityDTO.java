package com.ccomp.br.domain.events.dto.activities;

import jakarta.validation.constraints.Size;

public record CreateActivityDTO(
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description
) {
}
