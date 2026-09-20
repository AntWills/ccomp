package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_event_guests", indexes = {
        @Index(name = "idx_event_guest_user_id_event_id", columnList = "user_id, event_id")
})
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventGuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tb_event_guests_event")
    )
    private Event event;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnumGuestStatus status = EnumGuestStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnumGuestVisibility visibility = EnumGuestVisibility.PUBLIC;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isPublic() {
        return this.visibility == EnumGuestVisibility.PUBLIC;
    }

    public boolean isConfirmed() {
        return this.status == EnumGuestStatus.CONFIRMED;
    }
}
