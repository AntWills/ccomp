package com.ccomp.br.domain.events.guests.dto;

/** Internal projection carrying the relevance score used by cursor pagination. */
public record EventGuestSearchResult(EventGuestDTO guest, Double relevance) {
}
