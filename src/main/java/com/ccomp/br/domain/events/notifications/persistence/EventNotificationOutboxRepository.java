package com.ccomp.br.domain.events.notifications.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventNotificationOutboxRepository extends JpaRepository<EventNotificationOutbox, Long> {
    long countByNotificationIdAndPublishedAtIsNull(Long notificationId);
    Optional<EventNotificationOutbox> findFirstByNotificationIdAndPublishedAtIsNullOrderByIdDesc(Long notificationId);
}
