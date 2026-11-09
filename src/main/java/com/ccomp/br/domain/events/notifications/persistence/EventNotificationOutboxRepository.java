package com.ccomp.br.domain.events.notifications.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventNotificationOutboxRepository extends JpaRepository<EventNotificationOutbox, Long> {
    List<EventNotificationOutbox> findTop100ByPublishedAtIsNullOrderByIdAsc();
    long countByNotificationIdAndPublishedAtIsNull(Long notificationId);
    Optional<EventNotificationOutbox> findFirstByNotificationIdAndPublishedAtIsNullOrderByIdDesc(Long notificationId);
}
