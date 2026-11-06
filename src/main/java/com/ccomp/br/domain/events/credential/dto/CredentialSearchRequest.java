package com.ccomp.br.domain.events.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialSearchRequest(@NotBlank @Size(min = 2, max = 100) String searchTerm) {}
