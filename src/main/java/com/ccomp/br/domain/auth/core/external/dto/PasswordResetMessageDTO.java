package com.ccomp.br.domain.auth.core.external.dto;

import com.ccomp.br.module.email.EmailAddress;

public record PasswordResetMessageDTO(
        EmailAddress email,
        String token
) {
}
