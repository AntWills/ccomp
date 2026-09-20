package com.ccomp.br.domain.auth.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResetPasswordRequestDTO (
        @NotNull(message = "O token é o obrigatório.")
        @NotBlank(message = "O token é o obrigatório.")
        String token,
        @NotNull(message = "Password is required.")
        @NotBlank(message = "Password is required.")
        String password
) {
}
