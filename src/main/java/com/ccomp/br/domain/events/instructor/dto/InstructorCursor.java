package com.ccomp.br.domain.events.instructor.dto;
import java.time.LocalDateTime;

public record InstructorCursor(Double relevance, Long id, LocalDateTime createdAt) {
    public InstructorCursor(Long id, LocalDateTime createdAt) {
        this(null, id, createdAt);
    }
}
