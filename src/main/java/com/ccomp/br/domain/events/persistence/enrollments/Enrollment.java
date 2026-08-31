package com.ccomp.br.domain.events.persistence.enrollments;

import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.shared.exceptions.DomainException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "tb_event_enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_user",
                        columnNames = {"events_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_enrollment_user", columnList = "user_id"),
                @Index(name = "idx_enrollment_event_status", columnList = "events_id, status")
        }
)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "events_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)", nullable = false)
    @Builder.Default
    private EnumEnrollmentState status = EnumEnrollmentState.CONFIRMED;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Enrollment(UUID userId, Event event) {
        this.userId = userId;
        this.event = event;
        this.status = EnumEnrollmentState.CONFIRMED;
    }

    // --- Métodos de Domínio para Transição de Estado ---

    /**
     * Realiza a confirmação de presença do participante no evento.
     */
    public void checkIn() {
        if (this.status == EnumEnrollmentState.CANCELED) {
            throw new DomainException("Não é possível realizar credenciamento em uma inscrição cancelada.");
        }
        this.status = EnumEnrollmentState.CHECKED_IN;
        this.checkInAt = LocalDateTime.now();
    }

    /**
     * Cancela a inscrição do participante.
     */
    public void cancel() {
        if (this.status == EnumEnrollmentState.CHECKED_IN) {
            throw new DomainException("Não é possível cancelar uma inscrição que já possui presença confirmada.");
        }
        this.status = EnumEnrollmentState.CANCELED;
    }

    /**
     * Retorna se a inscrição está ocupando uma vaga ativa no evento.
     */
    public boolean isActive() {
        return this.status == EnumEnrollmentState.CONFIRMED || this.status == EnumEnrollmentState.CHECKED_IN;
    }
}