package com.ccomp.br.domain.clubs.dto;

public record UpdateClubRequestDTO(
        String name,
        String summary,
        String coverImageUrl,
        String content
) {
}
