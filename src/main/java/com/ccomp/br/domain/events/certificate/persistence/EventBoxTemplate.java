package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Table(name = "tb_event_certificate_box_template", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_certificate_box_type", columnNames = {"event_id", "template_type"})
})
@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventBoxTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 20)
    @Builder.Default
    private EnumEventCertificateTemplateType templateType = EnumEventCertificateTemplateType.PARTICIPANT;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> variables;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<BoxElementDTO> boxes;
}
