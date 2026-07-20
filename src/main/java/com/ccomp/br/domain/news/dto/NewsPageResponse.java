package com.ccomp.br.domain.news.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NewsPageResponse(
        List<NewsListItem> items,
        LocalDateTime nextCursor,
        boolean hasNext
) {
}
