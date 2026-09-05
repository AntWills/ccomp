package com.ccomp.br.domain.events.persistence.editors.validation;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_event_editor_invitations")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventEditorInvitations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID code;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email_address", nullable = false, unique = true)
    )
    private EmailAddress emailAddress;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
