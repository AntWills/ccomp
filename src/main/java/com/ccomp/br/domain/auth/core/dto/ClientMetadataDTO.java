package com.ccomp.br.domain.auth.core.dto;

import lombok.Builder;

@Builder
public record ClientMetadataDTO(
        String ipAddress,
        String userAgent
) {
}
