package com.ccomp.br.domain.auth.external.dto;

import com.ccomp.br.module.email.EmailAddress;

public record PasswordResetRequestedEvent(
        EmailAddress email,
        String token
) {
}
