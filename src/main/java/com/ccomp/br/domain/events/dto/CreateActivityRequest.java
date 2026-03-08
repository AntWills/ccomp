package com.ccomp.br.domain.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateActivityRequest(
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description
) {
}
