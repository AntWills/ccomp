package com.ccomp.br.domain.events.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BadgePrintRequest(@NotBlank @Size(max = 50) String station) {}
