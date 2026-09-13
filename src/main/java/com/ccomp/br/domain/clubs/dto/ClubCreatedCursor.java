package com.ccomp.br.domain.clubs.dto;

import java.time.LocalDateTime;

public record ClubCreatedCursor(
        Long id,
        LocalDateTime createdAt
) {}
