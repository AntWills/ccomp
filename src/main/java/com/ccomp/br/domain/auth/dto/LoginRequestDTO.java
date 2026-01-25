package com.ccomp.br.domain.auth.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "Email is required.")
        EmailAddress email,

        @NotNull(message = "Password is required.")
        @NotBlank(message = "Password is required.")
        String password
) {
}
