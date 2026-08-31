package com.ccomp.br.domain.events.dto.activities;

import jakarta.validation.constraints.Size;

public record UpdateActivityDTO(
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String description,
        
        Long displayOrder
) {
}
