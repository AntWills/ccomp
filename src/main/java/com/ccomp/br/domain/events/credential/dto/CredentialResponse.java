package com.ccomp.br.domain.events.credential.dto;

import java.time.LocalDateTime;

public record CredentialResponse(
        String result,
        Long credentialId,
        Long enrollmentId,
        String enrollmentStatus,
        LocalDateTime credentialedAt,
        boolean printBadge,
        String participantName,
        String emailMasked
) {}
