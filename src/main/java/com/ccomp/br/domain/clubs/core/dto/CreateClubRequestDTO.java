package com.ccomp.br.domain.clubs.core.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record CreateClubRequestDTO(
        String name,
        String summary,
        @PositiveOrZero Long maxMembers
) {
    public CreateClubRequestDTO(String name, String summary) {
        this(name, summary, null);
    }
}
