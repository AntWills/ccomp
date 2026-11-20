package com.ccomp.br.domain.events.instructor.dto;

/** Internal projection carrying the relevance score used by cursor pagination. */
public record InstructorSearchResult(EventInstructorDTO instructor, Double relevance) {
}
