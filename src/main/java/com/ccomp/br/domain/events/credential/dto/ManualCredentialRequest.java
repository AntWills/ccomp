package com.ccomp.br.domain.events.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualCredentialRequest(
        @NotNull Long enrollmentId,
        @NotBlank @Size(max = 50) String station
) {}
