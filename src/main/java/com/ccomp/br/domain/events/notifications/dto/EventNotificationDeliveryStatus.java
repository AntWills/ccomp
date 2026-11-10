package com.ccomp.br.domain.events.notifications.dto;

import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;

public record EventNotificationDeliveryStatus(Long notificationId, NotificationStatus status,
                                              long recipientCount, long emailPending,
                                              long emailSent, long emailFailed,
                                              long outboxPending, String lastOutboxError) {}
