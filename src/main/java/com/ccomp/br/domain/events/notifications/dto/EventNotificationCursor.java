package com.ccomp.br.domain.events.notifications.dto;

import java.time.LocalDateTime;

public record EventNotificationCursor(LocalDateTime createdAt, Long id) {}
