package com.ccomp.br.domain.events.persistence.editors;

import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.persistence.Event;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_event_editors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tb_event_editors_event_user", columnNames = {"event_id", "userId"})
        })
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)", nullable = false)
    private EnumEditorsStatus status;

    public boolean isActive() {
        return status == EnumEditorsStatus.ACTIVE;
    }
}
