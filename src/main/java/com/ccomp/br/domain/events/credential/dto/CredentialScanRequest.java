package com.ccomp.br.domain.events.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialScanRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(max = 50) String station
) {}
