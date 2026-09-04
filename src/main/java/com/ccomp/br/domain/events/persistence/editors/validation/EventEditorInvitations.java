package com.ccomp.br.domain.events.persistence.editors.validation;

import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email_address", nullable = false, unique = true)
    )
    private EmailAddress emailAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_editor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EventEditor eventEditor;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
