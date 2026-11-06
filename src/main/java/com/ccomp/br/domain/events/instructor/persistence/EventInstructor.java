package com.ccomp.br.domain.events.instructor.persistence;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Table(name = "tb_event_instructors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_instructor_activity_user", columnNames = {"activity_id", "user_id"})
        })
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventInstructor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnumInstructorStatus status = EnumInstructorStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnumInstructorRole role = EnumInstructorRole.INSTRUCTOR;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isConfirmed() {
        return status == EnumInstructorStatus.CONFIRMED;
    }

    public boolean isCanceled() {
        return status == EnumInstructorStatus.CANCELED;
    }

    public void reactivate() {
        if (!isCanceled()) {
            throw new IllegalStateException("Somente um vínculo cancelado pode ser reativado.");
        }
        this.status = EnumInstructorStatus.CONFIRMED;
    }

    public void cancel() {
        if (isCanceled()) {
            return;
        }
        this.status = EnumInstructorStatus.CANCELED;
    }

}
