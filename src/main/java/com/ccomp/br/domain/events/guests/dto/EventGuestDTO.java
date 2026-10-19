package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventGuestDTO(
        Long id,
        Long eventId,
        UUID userId,
        EnumGuestStatus status,
        EnumGuestVisibility visibility,
        LocalDateTime createdAt
) {
}
