package com.comp.br.modules.users.dto;

import java.util.UUID;

public record UserRes(
        UUID id,
        String name
) {
}
