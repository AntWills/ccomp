package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_activity_activity")
    )
    private EventActivity activity;

    public EnrollmentActivity(Enrollment enrollment, EventActivity activity) {
        this.enrollment = enrollment;
        this.activity = activity;
    }
}
