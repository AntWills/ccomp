package com.ccomp.br.domain.clubs.core.dto;

import com.ccomp.br.domain.clubs.core.enums.EnumClubStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "Dados de retorno de um clube")
public record ClubResponseDTO(
        Long id,
        String name,
        String summary,
        EnumClubStatus status,
        String coverImageUrl,
        String content,
        Long maxMembers,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) implements Serializable {
}
