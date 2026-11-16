package com.ccomp.br.domain.clubs.certificates.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_club_certificate_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubCertificateSettings {
    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "member_workload_hours", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal memberWorkloadHours = BigDecimal.ZERO;

    @Column(name = "instructor_workload_hours", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal instructorWorkloadHours = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
