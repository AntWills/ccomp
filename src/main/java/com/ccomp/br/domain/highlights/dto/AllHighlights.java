package com.ccomp.br.domain.highlights.dto;

import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.shared.dto.EventListItemView;
import com.ccomp.br.domain.news.dto.NewsItem;

import java.util.List;

public record AllHighlights(
        List<ClubResponseDTO> clubs,
        List<NewsItem> news,
        List<EventListItemView> events
) {
}
