package com.ccomp.br.domain.auth.core.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequestDTO(
        @NotNull(message = "O e-mail é obrigatório.")
        @Valid
        EmailAddress email
) {
}
