package com.ccomp.br.domain.observability.dto;

import java.time.Instant;
import java.util.List;

public record InfraLogsResponse(
        List<InfraLogEntry> logs,
        Instant start,
        Instant end,
        int limit
) {
}
