package com.ccomp.br.domain.news.dto;

import com.ccomp.br.domain.news.persistence.News;

import java.util.List;

public record UserNewsResponseDTO(
        List<News> author,
        List<News> editor
) {
}
