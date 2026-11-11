package com.ccomp.br.domain.events.enrollments.dto;

/** Internal projection that carries the score needed to continue relevance ordered pagination. */
public record EnrollmentSearchResult(EnrollmentListItem enrollment, Double relevance) {
}
