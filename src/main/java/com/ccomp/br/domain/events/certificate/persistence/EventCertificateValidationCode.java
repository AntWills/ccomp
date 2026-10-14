package com.ccomp.br.domain.events.certificate.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "tb_event_certificate_validation_code",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cert_validation_code",
                        columnNames = {"code"}
                ),
                @UniqueConstraint(
                        name = "uk_cert_validation_event_activity_user",
                        columnNames = {"event_id", "activity_id", "user_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_cert_validation_event_user",
                        columnList = "event_id, user_id"
                ),
                @Index(
                        name = "idx_cert_validation_activity_user",
                        columnList = "activity_id, user_id"
                )
        }
)
@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventCertificateValidationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}