package com.ccomp.br.domain.highlights.application;

import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.highlights.dto.AllHighlights;
import com.ccomp.br.domain.news.application.NewsApplication;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.ccomp.br.domain.news.dto.NewsSearchFilter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HighlightsApplication {
    private final ClubService clubService;
    private final NewsApplication newsApplication;
    private final EventsServices eventsServices;

    public HighlightsApplication(ClubService clubService, NewsApplication newsApplication, EventsServices eventsServices) {
        this.clubService = clubService;
        this.newsApplication = newsApplication;
        this.eventsServices = eventsServices;
    }

    @Cacheable(cacheNames = "public-highlights", key = "'all'")
    public AllHighlights all() {
        return new AllHighlights(clubs(), news(), events());
    }

    @Cacheable(cacheNames = "public-highlight-clubs", key = "'top'")
    public List<ClubResponseDTO> clubs() {
        return clubService.search(null, 3).content();
    }

    @Cacheable(cacheNames = "public-highlight-news", key = "'top'")
    public List<NewsItem> news() {
        return newsApplication.searchNewsWithFilters(new NewsSearchFilter(null), null, 3).content();
    }

    @Cacheable(cacheNames = "public-highlight-events", key = "'top'")
    public List<EventListItemDTO> events() {
        return eventsServices.searchEventsWithFilters(new EventsFilterRequest(null, null), null, 3).content();
    }
}
