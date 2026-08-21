package com.ccomp.br.shared.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;

import java.time.LocalDateTime;

public record EventListItem (
        Long id,
        String title,
        String slug,
        String summary,
        EnumEventFormat format,
        EnumEventCategory category,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
