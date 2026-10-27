package com.ccomp.br.domain.events.core.dto;

public record EventSearchResult(EventListItemDTO event, Double relevance) {
}
