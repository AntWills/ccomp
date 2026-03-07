package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.persistence.Event;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "tb_event_activities")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
}
