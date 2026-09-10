package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.enums.activities.EnumActivityAccessPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationRequirement;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;
import com.ccomp.br.domain.events.persistence.Event;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Table(name = "tb_event_activities")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EnumActivityType type;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Long displayOrder = 0L;

    @Column(length = 255)
    private String location;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_requirement", nullable = false)
    private EnumActivityRegistrationRequirement registrationRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_policy", nullable = false)
    private EnumActivityAccessPolicy accessPolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_activities_event")
    )
    private Event event;

    @OneToMany(
            mappedBy = "activity",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<EnrollmentActivity> activities = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void validateDates() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de término.");
        }
    }
}