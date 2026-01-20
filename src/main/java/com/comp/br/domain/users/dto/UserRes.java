package com.comp.br.domain.users.dto;

import com.comp.br.module.email.EmailAddress;

import java.util.UUID;

public record UserRes(
        UUID id,
        String name,
        EmailAddress email
) {
}
