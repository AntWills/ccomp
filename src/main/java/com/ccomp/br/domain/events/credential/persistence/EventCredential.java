package com.ccomp.br.domain.events.credential.persistence;

import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_event_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private Long enrollmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
    private Enrollment enrollment;

    @Column(name = "credentialed_at")
    private LocalDateTime credentialedAt;

    @Column(name = "credentialed_by")
    private UUID credentialedBy;

    @Column(length = 50)
    private String station;

    @Column(name = "badge_print_count", nullable = false)
    @Builder.Default
    private int badgePrintCount = 0;

    @Column(name = "last_badge_printed_at")
    private LocalDateTime lastBadgePrintedAt;

    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_updated_at")
    private LocalDateTime tokenUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void credential(String station, UUID actor, LocalDateTime at) {
        if (credentialedAt == null) {
            credentialedAt = at;
            credentialedBy = actor;
            this.station = station;
            updatedAt = at;
        }
    }

    public void rotateToken(String hash, LocalDateTime at) {
        tokenHash = hash;
        tokenUpdatedAt = at;
        updatedAt = at;
    }

    public void reprint(LocalDateTime at) {
        badgePrintCount++;
        lastBadgePrintedAt = at;
        updatedAt = at;
    }
}
