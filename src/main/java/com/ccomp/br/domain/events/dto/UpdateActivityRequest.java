package com.ccomp.br.domain.events.dto;

import jakarta.validation.constraints.Size;

public record UpdateActivityRequest(
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String description,
        
        Long displayOrder
) {
}
