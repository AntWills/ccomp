package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        EmailAddress email
) {
}
