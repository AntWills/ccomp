package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.persistence.Club;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados de retorno de um clube")
public record ClubResponseDTO(
        Long id,
        String name,
        String summary,
        String coverImageUrl,
        String content,
//        Long participantLimit,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}