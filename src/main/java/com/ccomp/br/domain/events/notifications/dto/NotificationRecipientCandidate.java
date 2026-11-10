package com.ccomp.br.domain.events.notifications.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationRecipientCandidate(UUID userId, String userName, String emailAddress,
                                             LocalDateTime createdAt, Long cursorId) {}
