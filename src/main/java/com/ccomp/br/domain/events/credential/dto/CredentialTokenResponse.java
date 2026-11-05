package com.ccomp.br.domain.events.credential.dto;

public record CredentialTokenResponse(Long eventId, Long enrollmentId, String token) {}
