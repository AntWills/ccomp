package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("update EventNotificationRecipient r set r.readAt = :readAt where r.userId = :userId and r.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") java.time.LocalDateTime readAt);

    @Modifying
    @Query("update EventNotificationRecipient r set r.emailStatus = :processing, r.emailAttempts = r.emailAttempts + 1, r.lastEmailAttemptAt = :attemptAt where r.id = :id and r.emailStatus = :pending")
    int claimPending(@Param("id") Long id, @Param("pending") NotificationEmailStatus pending,
                     @Param("processing") NotificationEmailStatus processing,
                     @Param("attemptAt") java.time.LocalDateTime attemptAt);
}
