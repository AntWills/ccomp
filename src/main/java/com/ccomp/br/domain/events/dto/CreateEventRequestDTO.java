package com.ccomp.br.domain.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record CreateEventRequestDTO(
        @NotNull
        @Size(min = 4, max = 255, message = "Name must be between 4 and 255 characters long.")
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public Optional<LocalDateTime> getStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> getEndDate() {
        return Optional.ofNullable(endDate);
    }
}
