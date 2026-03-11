package com.ccomp.br.domain.auth.dto;

public record AccessTokenResponse(
    String accessToken,
    String refreshToken
) {
}
