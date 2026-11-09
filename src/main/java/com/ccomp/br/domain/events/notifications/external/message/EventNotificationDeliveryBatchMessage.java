package com.ccomp.br.domain.events.notifications.external.message;

import java.util.List;

public record EventNotificationDeliveryBatchMessage(Long notificationId, List<Long> recipientIds) {}
