package com.ccomp.br.domain.news.dto;

import com.ccomp.br.domain.news.persistence.ContentBlock;

import java.time.LocalDateTime;
import java.util.List;

public record NewsResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        Boolean featured,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        String content
//        List<ContentBlock> blocks
) {
}
