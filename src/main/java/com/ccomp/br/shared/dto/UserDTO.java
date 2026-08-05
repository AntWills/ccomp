package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        EmailAddress emailAddress,

        @JsonIgnore
        String password
) {
}
