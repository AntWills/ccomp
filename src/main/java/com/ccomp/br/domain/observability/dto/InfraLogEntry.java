package com.ccomp.br.domain.observability.dto;

import java.time.Instant;

public record InfraLogEntry(
        Instant timestamp,
        String serviceName,
        String level,
        String message
) {
}
