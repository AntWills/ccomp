package com.ccomp.br.domain.events.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table(name = "tb_events")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "event")
    private Set<Enrollment> enrollments = new HashSet<>();

    @Column(name = "start_date")
    private LocalDate start;

    @Column(name = "end_date")
    private LocalDate end;

    @JoinColumn(name = "owner_id", nullable = false)
    private UUID ownerId;

    public Event(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }
}
