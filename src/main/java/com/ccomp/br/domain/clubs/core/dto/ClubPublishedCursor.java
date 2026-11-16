package com.ccomp.br.domain.clubs.core.dto;

import java.time.LocalDateTime;

public record ClubPublishedCursor(
        Long id,
        LocalDateTime publishedAt
) {}
