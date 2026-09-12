package com.ccomp.br.domain.news.dto;

import java.time.LocalDateTime;

public record NewsCursor (
        LocalDateTime publishedAt,
        Long id
) {
}
