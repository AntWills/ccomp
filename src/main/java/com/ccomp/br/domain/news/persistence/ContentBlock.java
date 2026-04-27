package com.ccomp.br.domain.news.persistence;

public record ContentBlock(
        int order,
        String type,
        String content,
        String url,
        String caption,
        String platform,
        String author
) {
}
