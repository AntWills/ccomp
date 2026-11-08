package com.ccomp.br.domain.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Horário atual e métricas de recursos da JVM")
public record ServerStatusResponse(
        @Schema(description = "Horário atual do servidor em UTC, no formato ISO-8601", example = "2026-11-08T12:30:00Z")
        Instant timestamp,
        @Schema(description = "Memória heap atualmente usada, em bytes", example = "67108864")
        long memoryUsedBytes,
        @Schema(description = "Limite máximo de memória heap, em bytes", example = "536870912")
        long memoryMaxBytes,
        @Schema(description = "Uso total de CPU do sistema em percentual; null quando indisponível", nullable = true, example = "23.5")
        Double cpuUsagePercent
) {
}
