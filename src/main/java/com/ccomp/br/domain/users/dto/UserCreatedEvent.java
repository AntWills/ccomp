package com.ccomp.br.domain.users.dto;

import com.ccomp.br.module.email.EmailAddress;

public record UserCreatedEvent(
        String name,
        EmailAddress emailAddress
) {
}
