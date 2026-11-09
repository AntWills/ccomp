package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventNotificationRecipientRepository extends JpaRepository<EventNotificationRecipient, Long> {
    boolean existsByNotificationIdAndUserId(Long notificationId, UUID userId);
    List<EventNotificationRecipient> findAllByNotificationIdAndUserIdIn(Long notificationId, Collection<UUID> userIds);
    List<EventNotificationRecipient> findAllByIdIn(Collection<Long> ids);
    List<EventNotificationRecipient> findAllByNotificationIdAndEmailStatus(Long notificationId, NotificationEmailStatus status);
    Optional<EventNotificationRecipient> findByIdAndUserId(Long id, UUID userId);
    List<EventNotificationRecipient> findAllByUserIdAndReadAtIsNull(UUID userId);
    long countByUserIdAndReadAtIsNull(UUID userId);
    long countByNotificationId(Long notificationId);
    long countByNotificationIdAndEmailStatus(Long notificationId, NotificationEmailStatus status);

}
