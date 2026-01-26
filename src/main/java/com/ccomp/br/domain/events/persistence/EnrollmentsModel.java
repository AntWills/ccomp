package com.ccomp.br.domain.events.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(name = "tb_enrollments")
@Entity(name = "EnrollmentsModel")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnrollmentsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "events_id", nullable = false)
    private EventsModel eventsModel;

    public EnrollmentsModel(UUID userId, EventsModel eventsModel){
        this.userId = userId;
        this.eventsModel = eventsModel;
    }
}
