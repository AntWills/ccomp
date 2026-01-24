package com.ccomp.br.domain.auth.dto;

public record AuthResponse(
    String accessToken,
    Long expiresIn
) {
}
