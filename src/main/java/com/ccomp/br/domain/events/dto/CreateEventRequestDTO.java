package com.ccomp.br.domain.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Optional;

public record CreateEventRequestDTO(
        @NotNull
        @Size(min = 4, max = 255, message = "O titulo deve ter entre 4 e 255 caracteres.")
        String title,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public Optional<LocalDateTime> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optionalEndDate() {
        return Optional.ofNullable(endDate);
    }
}
