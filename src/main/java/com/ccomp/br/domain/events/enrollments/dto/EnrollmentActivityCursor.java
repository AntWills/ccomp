package com.ccomp.br.domain.events.enrollments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentActivityCursor(
    Double relevance,
    Long id,
    LocalDateTime createdAt
) {
}
