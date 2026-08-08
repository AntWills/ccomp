package com.ccomp.br.domain.news.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NewsPageResponse(
        List<NewsItem> items,
        LocalDateTime nextCursor,
        boolean hasNext
) {
}
