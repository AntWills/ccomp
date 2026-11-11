package com.ccomp.br.domain.auth.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO (
        @NotNull(message = "O token é obrigatório.")
        @NotBlank(message = "O token é obrigatório.")
        String token,
        @NotNull(message = "A senha é obrigatória.")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 255, message = "A senha deve ter no máximo 255 caracteres.")
        String password
) {
}
