package com.ccomp.br.domain.highlights.web;

import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.events.application.EventsServices;
import com.ccomp.br.domain.events.dto.EventsFilterRequest;
import com.ccomp.br.domain.highlights.dto.AllHighlights;
import com.ccomp.br.domain.news.application.NewsApplication;
import com.ccomp.br.domain.news.dto.NewsFilter;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.ccomp.br.shared.dto.EventListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Em Alta (Highlights)",
        description = "Operações relacionadas à busca de nóticias, clubes e eventos em alta.")
@RestController
@RequestMapping("api/highlights")
public class HighlightsController {
    private final ClubService clubService;
    private final NewsApplication newsApplication;
    private final EventsServices eventsServices;

    public HighlightsController(ClubService clubService, NewsApplication newsApplication, EventsServices eventsServices) {
        this.clubService = clubService;
        this.newsApplication = newsApplication;
        this.eventsServices = eventsServices;
    }

    @Operation(
            summary = "Lista tudo que está em destaque",
            description = "Retorna uma lista resumida (máximo de 3 itens de cada categoria) das nóticas, eventos e clubes em destaque na plataforma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destaques retornados com sucesso")
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<AllHighlights> highlights() {
        return ResponseEntity.ok(
                new AllHighlights(
                        clubService.search(null, 3).content(),
                        newsApplication.searchNewsWithFilters(new NewsFilter(null), null, 3).content(),
                        eventsServices.searchEventsWithFilters(new EventsFilterRequest(null, null), null, 3).content()
                )
        );
    }

    @Operation(
            summary = "Lista os clubes em destaque",
            description = "Retorna uma lista resumida (máximo de 3 itens) dos clubes em destaque na plataforma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destaques retornados com sucesso")
    })
    @SecurityRequirements
    @GetMapping("clubs")
    public ResponseEntity<List<ClubResponseDTO>> highlightsClubs() {
        return ResponseEntity.ok(
                clubService.search(null, 3).content()
        );
    }

    @Operation(
            summary = "Lista as notícias em destaque",
            description = "Retorna uma lista resumida (máximo de 3 itens) das notícias em destaque na plataforma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destaques retornados com sucesso")
    })
    @SecurityRequirements
    @GetMapping("news")
    public ResponseEntity<List<NewsItem>> highlightsNews() {
        return ResponseEntity.ok(
                newsApplication.searchNewsWithFilters(new NewsFilter(null), null, 3).content()
        );
    }

    @Operation(
            summary = "Lista os eventos em destaque",
            description = "Retorna uma lista resumida (máximo de 3 itens) dos eventos em destaque na plataforma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destaques retornados com sucesso")
    })
    @SecurityRequirements
    @GetMapping("events")
    public ResponseEntity<List<EventListItem>> highlightsEvents() {
        return ResponseEntity.ok(
                eventsServices.searchEventsWithFilters(new EventsFilterRequest(null, null), null, 3).content()
        );
    }
}
