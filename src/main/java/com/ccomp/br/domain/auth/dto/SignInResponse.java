package com.ccomp.br.domain.auth.dto;

public record SignInResponse(
    String accessToken,
    String refreshToken
) {
}
