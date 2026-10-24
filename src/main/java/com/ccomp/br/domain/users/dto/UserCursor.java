package com.ccomp.br.domain.users.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCursor(
        Double similarity,
        LocalDateTime createdAt,
        UUID id
) {
    public UserCursor(LocalDateTime createdAt, UUID id) {
        this(null, createdAt, id);
    }
}
