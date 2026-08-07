package com.ccomp.br.domain.users.external.dto;

import com.ccomp.br.module.email.EmailAddress;

public record UserCreatedEvent(
        String name,
        EmailAddress emailAddress
) {
}
