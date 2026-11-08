package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "tb_event_certificate_validation_code",
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

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 20)
    @Builder.Default
    private EnumCertificateType certificateType = EnumCertificateType.EVENT;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
