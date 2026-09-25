package com.ccomp.br.domain.events.core.dto;


import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

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

