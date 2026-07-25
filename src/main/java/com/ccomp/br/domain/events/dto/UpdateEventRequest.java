package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Optional;

public record UpdateEventRequest(
        @NotNull
        Long id,

        @Size(min = 4, max = 255, message = "O titulo deve ter entre 4 e 255 caracteres.")
        String title,
        @Size(min = 4, max = 1000, message = "A descrição deve ter entre 4 e 255 caracteres.")
        String description,
        EnumEventCategory eventCategory,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public Optional<String> optionalTitle() { return Optional.ofNullable(title); }

    public Optional<EnumEventCategory> optionalEnumEventCategory() { return Optional.ofNullable(eventCategory); }

    public Optional<LocalDateTime> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optionalEndDate() {
        return Optional.ofNullable(endDate);
    }
}
