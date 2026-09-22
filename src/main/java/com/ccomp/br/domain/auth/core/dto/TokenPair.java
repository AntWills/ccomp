package com.ccomp.br.domain.auth.core.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TokenPair(
    String accessToken,
    UUID refreshToken
) {
}
