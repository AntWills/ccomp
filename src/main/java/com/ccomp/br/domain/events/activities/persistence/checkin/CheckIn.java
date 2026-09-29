package com.ccomp.br.domain.events.activities.persistence.checkin;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private UUID code;

    public boolean checkCode(UUID other) {
        return other.equals(code);
    }
}
