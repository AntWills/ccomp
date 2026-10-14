package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Table(name = "tb_event_activity_certificate_box_template")
@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventActivityBoxTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> variables;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<BoxElementDTO> boxes;
}
