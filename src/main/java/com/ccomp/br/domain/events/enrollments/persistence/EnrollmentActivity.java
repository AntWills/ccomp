package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tb_event_enrollment_activities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollment_activity",
                        columnNames = {"enrollment_id", "activity_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_enrollment_activity_enrollment",
                        columnList = "enrollment_id"
                ),
                @Index(
                        name = "idx_enrollment_activity_activity",
                        columnList = "activity_id"
                ),
                @Index(
                        name = "idx_enrollment_activity_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "enrollment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_activity_enrollment")
    )
    private Enrollment enrollment;

    @Column(name = "enrollment_id", nullable = false, updatable = false, insertable = false)
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_activity_activity")
    )
    private EventActivity activity;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @Column(name = "canceled", nullable = false)
    @Builder.Default
    private boolean canceled = false;

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EnrollmentActivity(Enrollment enrollment, EventActivity activity) {
        this.enrollment = enrollment;
        this.activity = activity;
    }

    public void makeAttendance(LocalDateTime now) {
        attendedAt = now;
    }

    public boolean haveCheckIn() {
        return attendedAt != null;
    }

    public void cancel(UUID managerId) {
        canceled = true;
        canceledBy = managerId;
    }

    public void restore() {
        canceled = false;
        canceledBy = null;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
