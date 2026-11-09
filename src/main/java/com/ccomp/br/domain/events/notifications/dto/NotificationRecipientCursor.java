package com.ccomp.br.domain.events.notifications.dto;

import java.time.LocalDateTime;

public record NotificationRecipientCursor(LocalDateTime createdAt, Long id) {}
