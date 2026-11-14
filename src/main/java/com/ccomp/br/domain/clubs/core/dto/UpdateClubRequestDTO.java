package com.ccomp.br.domain.clubs.core.dto;

import com.ccomp.br.domain.clubs.core.enums.EnumClubStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record UpdateClubRequestDTO(
        String name,
        String summary,
        EnumClubStatus status,
        String coverImageUrl,
//        Long participantLimit,
        LocalDateTime publishedAt,
        String content,
        @PositiveOrZero Long maxMembers
) {
    public UpdateClubRequestDTO(String name, String summary, EnumClubStatus status,
                                String coverImageUrl, LocalDateTime publishedAt, String content) {
        this(name, summary, status, coverImageUrl, publishedAt, content, null);
    }
}
