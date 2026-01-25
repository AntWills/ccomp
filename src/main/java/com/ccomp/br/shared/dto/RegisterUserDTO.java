package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserDTO(
    @NotNull(message = "Name is required.")
    @Size(min = 4, max = 255, message = "Name must between 4 and 255 characters.")
    String name,

    @NotNull(message = "Email is required.")
    @Valid
    EmailAddress email,

    @NotNull(message = "Password is required.")
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 255, message = "Password is short.")
    String password
) {
}
