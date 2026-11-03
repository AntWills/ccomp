package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;

import java.time.LocalDateTime;

public record ActivityGuestDTO(
        Long id,
        Long activityId,
        Long eventGuestId,
        String name,
        String description,
        String imageKey,
        EnumGuestStatus status,
        EnumGuestVisibility visibility,
        LocalDateTime createdAt
) {
}
