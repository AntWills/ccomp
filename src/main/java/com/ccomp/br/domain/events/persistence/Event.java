package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.shared.exceptions.DomainException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "tb_events", indexes = {
        @Index(name = "idx_events_category", columnList = "category"),
        @Index(name = "idx_events_format", columnList = "format"),
        @Index(name = "idx_events_status", columnList = "status"),
        @Index(name = "idx_events_start_date", columnList = "start_date"),
        @Index(name = "idx_events_location", columnList = "latitude, longitude")
})
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Size(max = 500, message = "O máximo são 500 caracteres.")
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", columnDefinition = "varchar(25)", nullable = false)
    private EnumEventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", columnDefinition = "varchar(20)", nullable = false)
    private EnumEventFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)", nullable = false)
    @Builder.Default
    private EnumEventStatus status = EnumEventStatus.DRAFT;

    private String coverImageUrl;

    // --- Inscrições e Vagas ---
    @Column(name = "enrollment_start_date")
    private LocalDateTime enrollmentStartDate;

    @Column(name = "enrollment_end_date")
    private LocalDateTime enrollmentEndDate;

    @Column(name = "enrollment_paused", nullable = false)
    @Builder.Default
    private boolean enrollmentPaused = false;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @OneToMany(mappedBy = "event")
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "event")
    private Set<EventEditor> editors;

    @OneToMany(mappedBy = "event")
    private List<EventActivity> activities;

    // --- Execução do Evento ---
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "online_url")
    private String onlineUrl;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Event(String title, UUID ownerId) {
        this.title = title;
        this.ownerId = ownerId;
        this.status = EnumEventStatus.DRAFT;
    }

    // --- Regras de Validação de Domínio ---
    private void verifyEventDates() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainException("A data de início do evento não pode ser posterior à data de término.");
        }
    }

    private void verifyEnrollmentDates() {
        if (enrollmentStartDate != null && enrollmentEndDate != null && enrollmentStartDate.isAfter(enrollmentEndDate)) {
            throw new DomainException("A data inicial das inscrições não pode ser posterior à data limite.");
        }
    }

    // --- Métodos Calculados do Ciclo de Vida ---

    /**
     * Retorna a fase de execução atual do evento com base no horário do sistema.
     */
    public EnumEventExecutionStatus getExecutionStatus() {
        if (startDate == null || endDate == null) {
            return EnumEventExecutionStatus.NOT_STARTED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) {
            return EnumEventExecutionStatus.NOT_STARTED;
        }
        if (now.isAfter(endDate)) {
            return EnumEventExecutionStatus.FINISHED;
        }
        return EnumEventExecutionStatus.IN_PROGRESS;
    }

    /**
     * Calcula dinamicamente o estado atual do período de inscrições.
     */
    public EnumEnrollmentStatus getEnrollmentStatus() {
        if (this.status == EnumEventStatus.DRAFT || this.status == EnumEventStatus.CANCELED) {
            return EnumEnrollmentStatus.CLOSED;
        }
        if (this.enrollmentPaused) {
            return EnumEnrollmentStatus.PAUSED;
        }
        if (this.maxCapacity != null && this.enrollments != null && this.enrollments.size() >= this.maxCapacity) {
            return EnumEnrollmentStatus.SOLD_OUT;
        }
        if (this.enrollmentStartDate == null || this.enrollmentEndDate == null) {
            return EnumEnrollmentStatus.CLOSED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(this.enrollmentStartDate)) {
            return EnumEnrollmentStatus.UPCOMING;
        }
        if (now.isAfter(this.enrollmentEndDate)) {
            return EnumEnrollmentStatus.CLOSED;
        }

        return EnumEnrollmentStatus.OPEN;
    }

    /**
     * Informa se o evento está aceitando inscrições no momento exato da verificação.
     */
    public boolean isEnrollmentOpen() {
        return getEnrollmentStatus() == EnumEnrollmentStatus.OPEN;
    }

    /**
     * Verifica se o evento está acessível publicamente (Público ou Oculto por link).
     */
    public boolean isPubliclyAccessible() {
        return this.status == EnumEventStatus.PUBLISHED || this.status == EnumEventStatus.UNLISTED;
    }

    /**
     * Verifica se o evento está publicado e indexável.
     */
    public boolean isPublished() {
        return this.status == EnumEventStatus.PUBLISHED;
    }

    public boolean isOwner(UUID userId) {
        return ownerId.equals(userId);
    }

    // --- Setters com Validação ---
    public void setStartDate(LocalDateTime start) {
        this.startDate = start;
        verifyEventDates();
    }

    public void setEndDate(LocalDateTime end) {
        this.endDate = end;
        verifyEventDates();
    }

    public void setEnrollmentStartDate(LocalDateTime start) {
        this.enrollmentStartDate = start;
        verifyEnrollmentDates();
    }

    public void setEnrollmentEndDate(LocalDateTime end) {
        this.enrollmentEndDate = end;
        verifyEnrollmentDates();
    }
}