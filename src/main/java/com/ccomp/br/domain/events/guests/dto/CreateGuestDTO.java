package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuestDTO(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description
) {}
