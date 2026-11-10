package com.ccomp.br.domain.events.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEventNotificationRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 10000) String message
) {}
