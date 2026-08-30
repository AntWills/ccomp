package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Optional;

public record UpdateEventRequest(
        @Size(min = 4, max = 255, message = "O titulo deve ter entre 4 e 255 caracteres.")
        String title,

        @Size(min = 4, max = 255, message = "A descrição deve ter entre 4 e 255 caracteres.")
        String summary,

        @Size(min = 4, max = 5000, message = "A descrição deve ter entre 4 e 5000 caracteres.")
        String content,

        EnumEventCategory category,
        EnumEventFormat format,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public Optional<String> optionalTitle() { return Optional.ofNullable(title); }

    public Optional<EnumEventCategory> optionalEnumEventCategory() { return Optional.ofNullable(category); }

    public Optional<LocalDateTime> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optionalEndDate() {
        return Optional.ofNullable(endDate);
    }
}
