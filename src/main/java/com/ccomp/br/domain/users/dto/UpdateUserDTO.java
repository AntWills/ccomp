package com.ccomp.br.domain.users.dto;

import java.util.Optional;

public record UpdateUserDTO(
        String name
) {
    public Optional<String> nameOpt() {
        return Optional.ofNullable(name);
    }
}
