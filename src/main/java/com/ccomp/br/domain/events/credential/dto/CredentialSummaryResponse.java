package com.ccomp.br.domain.events.credential.dto;

public record CredentialSummaryResponse(long eligible, long credentialed, long remaining) {}
