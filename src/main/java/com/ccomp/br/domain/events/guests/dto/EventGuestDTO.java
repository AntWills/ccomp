package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;

import java.time.LocalDateTime;

public record EventGuestDTO(
        Long id,
        Long eventId,
        String name,
        String description,
        String imageKey,
        EnumGuestStatus status,
        EnumGuestVisibility visibility,
        LocalDateTime createdAt
) {
}
