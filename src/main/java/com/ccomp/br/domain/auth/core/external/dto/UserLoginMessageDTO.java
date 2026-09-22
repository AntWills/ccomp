package com.ccomp.br.domain.auth.core.external.dto;

import com.ccomp.br.module.email.EmailAddress;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserLoginMessageDTO(
        UUID userId,
        EmailAddress email,
        String ipAddress,
        String userAgent,
        LocalDateTime timestamp
) {
}
