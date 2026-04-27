package com.ccomp.br.domain.news.persistence;

public record ContentBlock(
        Integer order,
        String type,
        String content,
        String url,
        String caption,
        String platform,
        String author
) {
}
