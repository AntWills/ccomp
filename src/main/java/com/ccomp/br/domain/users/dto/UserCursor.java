package com.ccomp.br.domain.users.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCursor(
        LocalDateTime createdAt,
        UUID id
) {
}
