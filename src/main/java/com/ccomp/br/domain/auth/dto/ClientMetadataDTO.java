package com.ccomp.br.domain.auth.dto;

import lombok.Builder;

@Builder
public record ClientMetadataDTO(
        String ipAddress,
        String userAgent
) {
}
