package com.ccomp.br.domain.events.core.persistence;

import com.ccomp.br.domain.events.core.enums.*;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.shared.exceptions.DomainException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "tb_events", indexes = {
        @Index(name = "idx_events_category", columnList = "category"),
        @Index(name = "idx_events_format", columnList = "format"),
        @Index(name = "idx_events_status", columnList = "status"),
        @Index(name = "idx_events_start_date_id", columnList = "start_date DESC, id DESC"),
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

    @Column(name = "time_zone", nullable = false, length = 64)
    @Builder.Default
    private String timeZone = "America/Sao_Paulo";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)", nullable = false)
    @Builder.Default
    private EnumEventStatus status = EnumEventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_conflict_policy", columnDefinition = "varchar(20)", nullable = false)
    @Builder.Default
    private EnumScheduleConflictPolicy scheduleConflictPolicy = EnumScheduleConflictPolicy.PREVENT;

    @Column(name = "cover_image_key")
    private String coverImageKey;

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

    @OneToMany(mappedBy = "event", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(
            mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<EventGuest> guests = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<EventEditor> editors;

    @OneToMany(mappedBy = "event", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<EventActivity> activities;

    // --- Execução do Evento ---
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "workload_hours")
    private BigDecimal workloadHours;

    @Column(name = "team_certificate_workload_hours")
    private BigDecimal teamCertificateWorkloadHours;

    @Column(name = "organization", length = 255)
    private String organization;

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
     * Retorna a fase de execução atual usando o horário fornecido pelo Clock central em Brasília.
     */
    public EnumEventExecutionStatus getExecutionStatus(LocalDateTime now) {
        if (startDate == null || endDate == null) {
            return EnumEventExecutionStatus.NOT_STARTED;
        }
        if (now.isBefore(startDate)) {
            return EnumEventExecutionStatus.NOT_STARTED;
        }
        if (!now.isBefore(endDate)) {
            return EnumEventExecutionStatus.FINISHED;
        }
        return EnumEventExecutionStatus.IN_PROGRESS;
    }

    /**
     * Calcula dinamicamente o estado atual do período de inscrições.
     */
    public EnumEnrollmentStatus getEnrollmentStatus(LocalDateTime now) {
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

        if (now.isBefore(this.enrollmentStartDate)) {
            return EnumEnrollmentStatus.UPCOMING;
        }
        if (!now.isBefore(this.enrollmentEndDate)) {
            return EnumEnrollmentStatus.CLOSED;
        }

        return EnumEnrollmentStatus.OPEN;
    }

    /**
     * Informa se o evento está aceitando inscrições no momento exato da verificação.
     */
    public boolean isEnrollmentOpen(LocalDateTime now) {
        return getEnrollmentStatus(now) == EnumEnrollmentStatus.OPEN;
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timeZone == null ? "America/Sao_Paulo" : timeZone);
        } catch (DateTimeException exception) {
            throw new DomainException("O fuso horário deve ser um identificador IANA válido, como America/Sao_Paulo.");
        }
    }

    public void setTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            throw new DomainException("O fuso horário do evento é obrigatório.");
        }
        try {
            this.timeZone = ZoneId.of(timeZone).getId();
        } catch (DateTimeException exception) {
            throw new DomainException("O fuso horário deve ser um identificador IANA válido, como America/Sao_Paulo.");
        }
    }

    @PrePersist
    @PreUpdate
    private void validateTimeZone() {
        getZoneId();
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

    public void publish(LocalDateTime now) {
        if (this.startDate == null || this.endDate == null) {
            throw new DomainException("Não é possível publicar um evento sem definir as datas de início e término.");
        }
        if (this.getExecutionStatus(now) == EnumEventExecutionStatus.FINISHED) {
            throw new DomainException("Não é possível publicar um evento que já foi finalizado.");
        }
        this.status = EnumEventStatus.PUBLISHED;
    }

    public void unlist(LocalDateTime now) {
        if (this.startDate == null || this.endDate == null) {
            throw new DomainException("Não é possível tornar um evento não listado sem definir as datas de início e término.");
        }
        if (this.getExecutionStatus(now) == EnumEventExecutionStatus.FINISHED) {
            throw new DomainException("Não é possível alterar para não listado um evento que já foi finalizado.");
        }
        this.status = EnumEventStatus.UNLISTED;
    }

    public void cancel(LocalDateTime now) {
        if (this.getExecutionStatus(now) == EnumEventExecutionStatus.FINISHED) {
            throw new DomainException("Não é possível cancelar um evento que já foi finalizado.");
        }
        this.status = EnumEventStatus.CANCELED;
    }

    public void moveToDraft() {
        // Exemplo: impede voltar para rascunho se já existirem inscrições ativas
        boolean hasActiveEnrollments = this.enrollments != null &&
                this.enrollments.stream().anyMatch(Enrollment::isActive);

        if (hasActiveEnrollments) {
            throw new DomainException("Não é possível alterar para Rascunho um evento que já possui participantes inscritos.");
        }
        this.status = EnumEventStatus.DRAFT;
    }

    public boolean isSchedulerPrevent() {
        return scheduleConflictPolicy == EnumScheduleConflictPolicy.PREVENT;
    }
}
