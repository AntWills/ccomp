package com.ccomp.br.domain.clubs.certificates.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ClubCertificateWorkloadRequest(
        @NotNull @DecimalMin("0.0") BigDecimal memberWorkloadHours,
        @NotNull @DecimalMin("0.0") BigDecimal instructorWorkloadHours
) {
}
