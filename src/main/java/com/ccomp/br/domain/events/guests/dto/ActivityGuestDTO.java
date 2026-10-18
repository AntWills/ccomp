package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityGuestDTO(
        Long id,
        Long activityId,
        Long eventGuestId,
        UUID userId,
        EnumGuestStatus status,
        EnumGuestVisibility visibility,
        LocalDateTime createdAt
) {
}
