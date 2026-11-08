package com.ccomp.br.domain.events.certificate.dto;

import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CertificateValidationResponse(
        String code,
        UUID participantId,
        String participantName,
        String eventTitle,
        String activityTitle,
        String workloadHours,
        LocalDateTime issuedAt,
        EnumCertificateType certificateType
) {
    public CertificateValidationResponse(String code, UUID participantId, String participantName,
                                         String eventTitle, String activityTitle, String workloadHours,
                                         LocalDateTime issuedAt) {
        this(code, participantId, participantName, eventTitle, activityTitle, workloadHours, issuedAt, null);
    }
}
