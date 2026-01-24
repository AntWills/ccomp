package com.comp.br.shared.dto;

import com.comp.br.module.email.EmailAddress;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        EmailAddress email
) {
}
