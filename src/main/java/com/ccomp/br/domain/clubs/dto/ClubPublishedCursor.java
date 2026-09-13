package com.ccomp.br.domain.clubs.dto;

import java.time.LocalDateTime;

public record ClubPublishedCursor(
        Long id,
        LocalDateTime publishedAt
) {}
