package com.ccomp.br.domain.events;

import com.ccomp.br.domain.users.persistence.UserModel;
import jakarta.persistence.*;

import java.time.LocalDate;

@Table(name = "tb_events")
@Entity(name = "EventsModel")
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

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserModel owner;
}
