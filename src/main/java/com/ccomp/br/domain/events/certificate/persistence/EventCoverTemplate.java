package com.ccomp.br.domain.events.certificate.persistence;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "tb_event_certificate_cover_template")
@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventCoverTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @Column(name = "cover_template_key", nullable = false)
    private String coverTemplateKey;
}
