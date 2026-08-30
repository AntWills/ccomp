package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;

import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        String coverImageUrl,
        EnumEventFormat format,
        EnumEventCategory category,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
