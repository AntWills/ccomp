package com.ccomp.br.domain.events.notifications.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyEventNotificationItem(Long id, Long notificationId, Long eventId, Long activityId,
                                      String title, String message,
                                      UUID senderUserId, String senderName, LocalDateTime sentAt, LocalDateTime readAt,
                                      boolean unread) {}
