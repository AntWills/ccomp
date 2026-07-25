package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.domain.news.enums.ContentBlockType;

public record ContentBlock(
        Long order,
        ContentBlockType type,
        String content,
        String url,
        String caption,
        String platform,
        String author
) {
}
