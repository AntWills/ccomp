package com.ccomp.br.domain.events.dto.activities;

import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;

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
