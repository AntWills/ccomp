package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.domain.news.enums.ContentBlockType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ContentBlock(
        Long order,

        @Schema(
                description = "Tipo do bloco de conteúdo. Valores aceitos: HEADING, PARAGRAPH, IMAGE, VIDEO, QUOTE",
                example = "HEADING"
        )
        ContentBlockType type,
        String content,
        String url,
        String caption,
        String platform,
        String author
) {
}
