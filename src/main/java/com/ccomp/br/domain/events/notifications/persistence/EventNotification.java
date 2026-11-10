package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tb_event_notifications",
        indexes = @Index(
                name = "idx_event_notifications_event_created_id",
                columnList = "event_id, created_at DESC, id DESC"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationAudience audience;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "recipient_count", nullable = false)
    private Integer recipientCount;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "dispatch_completed_at")
    private LocalDateTime dispatchCompletedAt;
}
