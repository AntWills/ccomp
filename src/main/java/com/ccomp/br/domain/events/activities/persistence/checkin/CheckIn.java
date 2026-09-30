package com.ccomp.br.domain.events.activities.persistence.checkin;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_events_check_in", indexes = {
        @Index(name = "idx_events_check_in_activity_id", columnList = "activity_id")
})
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private EventActivity activity;

    @Column(name = "activity_id", nullable = false, insertable = false, updatable = false)
    private Long activityId;

    @Column(nullable = false)
    private UUID code;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
