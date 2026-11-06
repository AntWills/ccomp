package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record CreateGuestDTO(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Email @Size(max = 255) String accountEmail
) {
    public CreateGuestDTO(String name, String description) {
        this(name, description, null);
    }
}
