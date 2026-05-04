package com.ccomp.br.domain.news.dto;

import com.ccomp.br.domain.news.persistence.ContentBlock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NewsUpdateDto(
        @NotNull(message = "O ID é obrigatório.")
        Long id,

        @NotBlank(message = "O título não deve estar vazio.")
        String title,
        String summary,
        String coverImageUrl,
        Boolean featured,
        List<ContentBlock> blocks
) {
}
