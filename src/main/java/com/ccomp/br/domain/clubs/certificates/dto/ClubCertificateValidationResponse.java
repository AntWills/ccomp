package com.ccomp.br.domain.clubs.certificates.dto;

import com.ccomp.br.domain.clubs.certificates.enums.EnumClubCertificateType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClubCertificateValidationResponse(
        String code,
        Long clubId,
        String clubName,
        UUID recipientId,
        String recipientName,
        EnumClubCertificateType certificateType,
        BigDecimal workloadHours,
        LocalDateTime issuedAt
) {
}
