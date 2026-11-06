package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;

import java.time.LocalDateTime;

public record ActivityGuestDTO(
        Long id,
        Long activityId,
        Long eventGuestId,
        String name,
        String description,
        String imageKey,
        EnumActivityGuestRole role,
        EnumGuestStatus status,
        EnumGuestVisibility visibility,
        boolean isGuest,
        LocalDateTime createdAt
) {
    public ActivityGuestDTO(Long id, Long activityId, Long eventGuestId, String name,
                            String description, String imageKey, EnumActivityGuestRole role,
                            EnumGuestStatus status, EnumGuestVisibility visibility, LocalDateTime createdAt) {
        this(id, activityId, eventGuestId, name, description, imageKey, role, status, visibility, true, createdAt);
    }
}
