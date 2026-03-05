package com.ccomp.br.domain.events.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_event_editors")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventEditor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private boolean active = true;
}
