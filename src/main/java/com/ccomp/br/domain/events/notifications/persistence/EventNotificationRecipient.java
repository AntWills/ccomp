package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tb_event_notification_recipients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_notification_recipient",
                columnNames = {"notification_id", "user_id"}
        ),
        indexes = @Index(
                name = "idx_event_notification_recipient_user_created_id",
                columnList = "user_id, created_at DESC, id DESC"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventNotificationRecipient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Enumerated(EnumType.STRING) @Column(name = "email_status", nullable = false, length = 20)
    @Builder.Default
    private NotificationEmailStatus emailStatus = NotificationEmailStatus.PENDING;

    @Column(name = "email_attempts", nullable = false)
    @Builder.Default
    private int emailAttempts = 0;

    @Column(name = "email_error", columnDefinition = "TEXT")
    private String emailError;

    @Column(name = "last_email_attempt_at")
    private LocalDateTime lastEmailAttemptAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
