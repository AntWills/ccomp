package com.ccomp.br.domain.events.activities.dto;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;

import java.time.LocalDateTime;

public record ActivityDTO(
        Long id,
        Long eventId,
        String title,
        String description,
        Long displayOrder,
        String location,
        LocalDateTime startDate,
        LocalDateTime endDate,
        EnumActivityRegistrationPolicy registrationPolicy,
        EnumActivityType type
) {
}
