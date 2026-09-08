package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.enums.activities.ActivityAccessPolicy;
import com.ccomp.br.domain.events.enums.activities.ActivityRegistrationRequirement;
import com.ccomp.br.domain.events.persistence.Event;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

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
    private ActivityRegistrationRequirement registrationRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_policy", nullable = false)
    private ActivityAccessPolicy accessPolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}