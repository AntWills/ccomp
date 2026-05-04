package com.ccomp.br.domain.news.dto;

import com.ccomp.br.domain.news.persistence.ContentBlock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewsUpdateDto(
        @NotNull(message = "O ID é obrigatório.")
        Long id,

        @Size(min = 5, message = "O título não pode estar vazio.")
        String title,
        String summary,
        String coverImageUrl,
        Boolean featured,
        List<ContentBlock> blocks
) {
}
