package com.ccomp.br.domain.events.notifications.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_event_notification_outbox", indexes = @Index(name = "idx_event_notification_outbox_pending", columnList = "id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventNotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;
    @Column(name = "recipient_ids", columnDefinition = "TEXT")
    private String recipientIds;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(nullable = false)
    @Builder.Default private int attempts = 0;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
