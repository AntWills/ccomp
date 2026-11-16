package com.ccomp.br.domain.clubs.core.dto;

import java.time.LocalDateTime;

public record ClubCreatedCursor(
        Long id,
        LocalDateTime createdAt
) {}
