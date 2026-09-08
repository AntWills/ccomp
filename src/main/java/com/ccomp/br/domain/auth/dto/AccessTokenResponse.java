package com.ccomp.br.domain.auth.dto;

import java.util.UUID;

public record AccessTokenResponse(
    String accessToken,
    UUID refreshToken
) {
}
