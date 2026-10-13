package com.ccomp.br.domain.events.certificate.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CertificateValidationResponse(
        String code,
        UUID participantId,
        String participantName,
        String eventTitle,
        String activityTitle,
        String workloadHours,
        LocalDateTime issuedAt
) {
}
