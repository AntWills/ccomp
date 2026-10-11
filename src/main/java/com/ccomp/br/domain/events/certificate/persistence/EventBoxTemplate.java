package com.ccomp.br.domain.events.certificate.persistence;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "tb_event_box_template")
@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventBoxTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;
}
