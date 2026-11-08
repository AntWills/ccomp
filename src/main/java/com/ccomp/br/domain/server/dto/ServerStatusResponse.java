package com.ccomp.br.domain.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;

@Schema(description = "Horário atual e métricas de recursos da JVM")
public record ServerStatusResponse(
        @Schema(description = "Horário atual do servidor em UTC, no formato ISO-8601", example = "2026-11-08T12:30:00Z")
        Instant timestamp,
        @Schema(description = "Horário local usado pelo Clock central do backend; interprete com o campo time_zone.", example = "2026-11-08T09:30:00")
        LocalDateTime serverLocalTime,
        @Schema(description = "Zona efetiva configurada no Clock central do backend.", example = "America/Sao_Paulo")
        String timeZone,
        @Schema(description = "Memória heap atualmente usada, em bytes", example = "67108864")
        long memoryUsedBytes,
        @Schema(description = "Limite máximo de memória heap, em bytes", example = "536870912")
        long memoryMaxBytes,
        @Schema(description = "Percentual do limite máximo de heap usado pela JVM.", example = "12.5")
        double memoryUsedPercent,
        @Schema(description = "Uso total de CPU do sistema em percentual; null quando indisponível", nullable = true, example = "23.5")
        Double cpuUsagePercent
) {
}
