package com.comp.br.domain.users.dto;

import com.comp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
    @NotNull(message = "Name is required.")
    @Size(min = 4, max = 255, message = "Name must between 4 and 255 characters.")
    String name,

    @NotNull(message = "Email is required.")
    @Valid
    EmailAddress email
) {
}
