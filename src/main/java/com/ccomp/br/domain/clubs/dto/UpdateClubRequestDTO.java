package com.ccomp.br.domain.clubs.dto;

import java.time.LocalDateTime;

public record UpdateClubRequestDTO(
        String name,
        String summary,
        String coverImageUrl,
        LocalDateTime publishedAt,
        String content
) {
}
