package com.ccomp.br.domain.auth.external.dto;

import com.ccomp.br.module.email.EmailAddress;

public record PasswordResetMessageDTO(
        EmailAddress email,
        String token
) {
}
