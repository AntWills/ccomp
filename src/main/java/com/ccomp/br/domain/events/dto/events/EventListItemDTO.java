package com.ccomp.br.domain.events.dto.events;


import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enums.EnumEventStatus;

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
) {
}

