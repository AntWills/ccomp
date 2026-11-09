package com.ccomp.br.domain.events.notifications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record SendSelectedNotificationRequest(
        Long activityId,
        @NotEmpty List<UUID> recipientUserIds,
        @Valid @jakarta.validation.constraints.NotNull CreateEventNotificationRequest notification
) {}
