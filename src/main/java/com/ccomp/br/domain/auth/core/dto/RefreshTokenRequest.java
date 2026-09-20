package com.ccomp.br.domain.auth.core.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefreshTokenRequest(
        @NotNull(message = "O refresh token é o obrigatório")
        UUID refreshToken
) {
}
