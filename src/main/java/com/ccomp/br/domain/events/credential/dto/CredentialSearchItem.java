package com.ccomp.br.domain.events.credential.dto;

public record CredentialSearchItem(
        Long enrollmentId,
        String name,
        String emailMasked,
        String enrollmentStatus,
        boolean eligible,
        boolean credentialed
) {}
