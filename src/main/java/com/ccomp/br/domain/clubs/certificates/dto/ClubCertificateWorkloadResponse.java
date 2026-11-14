package com.ccomp.br.domain.clubs.certificates.dto;

import java.math.BigDecimal;

public record ClubCertificateWorkloadResponse(
        Long clubId,
        BigDecimal memberWorkloadHours,
        BigDecimal instructorWorkloadHours
) {
}
