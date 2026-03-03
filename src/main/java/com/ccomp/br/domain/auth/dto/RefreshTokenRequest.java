package com.ccomp.br.domain.auth.dto;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(
        @NotNull(message = "O refresh token é o obrigatório")
        String refreshToken
) {
}
