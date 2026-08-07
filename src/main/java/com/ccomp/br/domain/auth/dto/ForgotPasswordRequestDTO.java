package com.ccomp.br.domain.auth.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequestDTO(
        @NotNull(message = "Email is required.")
        @Valid
        EmailAddress email
) {
}
