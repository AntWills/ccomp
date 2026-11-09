package com.ccomp.br.domain.events.notifications.dto;

import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventNotificationListItem(Long id, Long eventId, Long activityId, UUID senderUserId, String senderName,
                                        NotificationAudience audience, String title, String message,
                                        NotificationStatus status, Integer recipientCount, LocalDateTime createdAt) {}
