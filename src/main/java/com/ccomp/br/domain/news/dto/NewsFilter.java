package com.ccomp.br.domain.news.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record NewsFilter(
        @Min(1) @Max(100) Integer limit,
        Boolean featured,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor) {
    public NewsFilter {
        if (limit == null) {
            limit = 10;
        }
    }
}
