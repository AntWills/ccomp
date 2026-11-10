package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "tb_activity_certificate_template", uniqueConstraints = {
        @UniqueConstraint(name = "uk_activity_certificate_template_context", columnNames = {"activity_id", "template_type"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCertificateTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 20)
    @Builder.Default
    private EnumActivityCertificateTemplateType templateType = EnumActivityCertificateTemplateType.PARTICIPANT;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> variables;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<BoxElementDTO> boxes;

    @Column(name = "cover_template_key", length = 2048)
    private String coverTemplateKey;
}
