package com.ccomp.br.domain.events.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Table(name = "tb_events")
@Entity(name = "EventsModel")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate start;

    @Column(name = "end_date")
    private LocalDate end;

    @JoinColumn(name = "owner_id", nullable = false)
    private UUID ownerId;

    public EventsModel(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }
}
