package com.ccomp.br.domain.news.dto;

import java.time.LocalDateTime;

public record NewsItem(
                Long id,
                String title,
                String summary,
                String slug,
                String coverImageUrl,
                LocalDateTime publishedAt,
                Boolean featured) {
}
