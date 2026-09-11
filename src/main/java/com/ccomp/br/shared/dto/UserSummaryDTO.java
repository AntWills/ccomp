package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public record UserSummaryDTO(
        UUID id,
        String name,
        EmailAddress emailAddress
) {
}
