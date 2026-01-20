package com.comp.br.domain.users.dto;

import java.util.UUID;

public record UserRes(
        UUID id,
        String name
) {
}
