package com.ccomp.br.domain.events.enrollments.dto;

/** Internal projection carrying the score used by relevance ordered pagination. */
public record ActivityEnrollmentSearchResult(UserActivitySummaryDTO enrollment, Double relevance) {
}
