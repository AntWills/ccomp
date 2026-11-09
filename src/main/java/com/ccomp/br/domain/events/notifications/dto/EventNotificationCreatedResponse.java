package com.ccomp.br.domain.events.notifications.dto;

import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;

public record EventNotificationCreatedResponse(Long id, NotificationStatus status, int recipientCount) {}
