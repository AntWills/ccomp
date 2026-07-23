package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;

import java.time.LocalDateTime;

public record EventListItem(
        String title,
        String slug,
        EnumEventCategory category,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
