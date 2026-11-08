package com.ccomp.br.domain.events.instructor.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorInvitationStatus;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_event_instructor_invitations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private EventActivity activity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnumInstructorInvitationStatus status = EnumInstructorInvitationStatus.SENT;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email_address", nullable = false)
    )
    private EmailAddress emailAddress;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isSent() {
        return status == EnumInstructorInvitationStatus.SENT;
    }

    public boolean isAccepted() {
        return status == EnumInstructorInvitationStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return status == EnumInstructorInvitationStatus.REJECTED;
    }

    public boolean isCanceled() {
        return status == EnumInstructorInvitationStatus.CANCELED;
    }

    public boolean isValid(LocalDateTime now) {
        return isSent() && !isExpired(now);
    }

    public void accept(LocalDateTime now) {
        requireSent();
        status = EnumInstructorInvitationStatus.ACCEPTED;
        respondedAt = now;
    }

    public void reject(LocalDateTime now) {
        requireSent();
        status = EnumInstructorInvitationStatus.REJECTED;
        respondedAt = now;
    }

    public void cancel() {
        requireSent();
        status = EnumInstructorInvitationStatus.CANCELED;
    }

    public boolean isSameEmail(EmailAddress emailAddress) {
        return this.emailAddress.equals(emailAddress);
    }

    private void requireSent() {
        if (!isSent()) {
            throw new IllegalStateException("Somente convites enviados podem mudar de status.");
        }
    }
}
