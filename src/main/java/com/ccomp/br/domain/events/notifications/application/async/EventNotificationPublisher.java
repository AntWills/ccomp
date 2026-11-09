package com.ccomp.br.domain.events.notifications.application.async;

import com.ccomp.br.domain.events.notifications.persistence.EventNotificationOutbox;
import com.ccomp.br.domain.events.notifications.persistence.EventNotificationOutboxRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventNotificationPublisher {
    private final EventNotificationOutboxRepository outboxRepository;

    public EventNotificationPublisher(EventNotificationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    public void dispatch(Long notificationId) {
        outboxRepository.save(EventNotificationOutbox.builder()
                .notificationId(notificationId).messageType("DISPATCH").build());
    }

    public void deliver(Long notificationId, List<Long> recipientIds) {
        if (recipientIds.isEmpty()) return;
        outboxRepository.save(EventNotificationOutbox.builder()
                .notificationId(notificationId).messageType("DELIVERY")
                .recipientIds(recipientIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build());
    }
}
