package com.ccomp.br.domain.events.core.dto;


import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "Projeção resumida usada nas listagens de eventos. Não contém workloadHours, teamCertificateWorkloadHours ou organization; consulte EventDTO nos endpoints de detalhe para esses campos.")
public record EventListItemDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        EnumEventCategory category,
        EnumEventFormat format,
        EnumEventStatus status,
        String coverImageUrl,

        LocalDateTime enrollmentStartDate,
        LocalDateTime enrollmentEndDate,
        boolean enrollmentPaused,

        @Schema(name = "start_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        String address,
        String onlineUrl,
        @Schema(name = "time_zone", description = "DEPRECATED. Retornado por compatibilidade; horários usam Brasília (America/Sao_Paulo).", example = "America/Sao_Paulo", deprecated = true)
        String timeZone
) implements Serializable {
    public EventListItemDTO(Long id, String title, String slug, String summary, String content,
                            EnumEventCategory category, EnumEventFormat format, EnumEventStatus status,
                            String coverImageUrl, LocalDateTime enrollmentStartDate, LocalDateTime enrollmentEndDate,
                            boolean enrollmentPaused, LocalDateTime startDate, LocalDateTime endDate,
                            String address, String onlineUrl) {
        this(id, title, slug, summary, content, category, format, status, coverImageUrl, enrollmentStartDate,
                enrollmentEndDate, enrollmentPaused, startDate, endDate, address, onlineUrl, "America/Sao_Paulo");
    }
}

