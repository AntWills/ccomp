package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(
        name = "tb_activity_guests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activity_guest", columnNames = {"activity_id", "event_guest_id"})
        }
        )
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ActivityGuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private EventActivity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_guest_id", nullable = false)
    private EventGuest eventGuest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
