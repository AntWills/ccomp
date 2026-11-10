package com.ccomp.br.domain.events.notifications.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventNotificationRepository extends JpaRepository<EventNotification, Long> {
    Optional<EventNotification> findByIdAndEventId(Long id, Long eventId);
}
