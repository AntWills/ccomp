package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "tb_activity_guests")
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
    @JoinColumn(
            name = "activity_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_guests_activity")
    )
    private EventActivity activity;

    @Column(nullable = false)
    private UUID userId;
}
