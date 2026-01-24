package com.comp.br.domain.auth.dto;

public record AuthResponse(
    String accessToken,
    Long expiresIn
) {
}
