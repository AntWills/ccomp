package com.ccomp.br.domain.auth.core.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "O e-mail é obrigatório.")
        @Valid
        EmailAddress email,

        @NotNull(message = "A senha é obrigatória.")
        @NotBlank(message = "A senha é obrigatória.")
        String password
) {
}
