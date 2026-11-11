package com.ccomp.br.domain.auth.core.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefreshTokenRequest(
        @NotNull(message = "O token de atualização é obrigatório.")
        UUID refreshToken
) {
}
