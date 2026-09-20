package com.ccomp.br.domain.events.guests.persistence.invitations;

import com.ccomp.br.domain.events.shared.enums.EnumInvitationStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tb_event_invitations",
        indexes = {
                @Index(name = "idx_event_invitations_event", columnList = "event_id"),
                @Index(name = "idx_event_invitations_invited_id", columnList = "invited_at DESC, id DESC")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_invitations_event")
    )
    private Event event;

    @Column(name = "user_id")
    private UUID userId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email_address", nullable = false)
    )
    private EmailAddress emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumInvitationStatus status;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    public boolean notIsAccepted() {
        return status != EnumInvitationStatus.ACCEPTED;
    }

    public boolean isValid() {
        if(status != EnumInvitationStatus.PENDING)
            return false;
        return !isExpired();
    }

    public boolean isSameEmail(EmailAddress other) {
        return other.equals(emailAddress);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = EnumInvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = EnumInvitationStatus.CANCELLED;
    }

    public void refuse() {
        status = EnumInvitationStatus.DECLINED;
    }
}
