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

        LocalDateTime startDate,
        LocalDateTime endDate,
        String address,
        String onlineUrl
) implements Serializable {
}

