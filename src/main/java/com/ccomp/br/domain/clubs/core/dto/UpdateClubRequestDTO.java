package com.ccomp.br.domain.clubs.core.dto;

import com.ccomp.br.domain.clubs.core.enums.EnumClubStatus;
import java.time.LocalDateTime;

public record UpdateClubRequestDTO(
        String name,
        String summary,
        EnumClubStatus status,
        String coverImageUrl,
//        Long participantLimit,
        LocalDateTime publishedAt,
        String content
) {
}
