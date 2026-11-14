package com.ccomp.br.domain.clubs.core.dto;

import java.time.LocalDateTime;

public record UpdateClubRequestDTO(
        String name,
        String summary,
        String coverImageUrl,
//        Long participantLimit,
        LocalDateTime publishedAt,
        String content
) {
}
