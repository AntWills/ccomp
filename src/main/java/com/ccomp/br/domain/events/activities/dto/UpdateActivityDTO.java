package com.ccomp.br.domain.events.activities.dto;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record UpdateActivityDTO(
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String description,

        Long displayOrder,

        @Size(max = 255)
        String location,

        LocalDateTime startDate,

        LocalDateTime endDate,

        @DecimalMin(value = "0.0", message = "A carga horária não pode ser negativa")
        BigDecimal workloadHours,

        EnumActivityRegistrationPolicy registrationPolicy,

        EnumActivityType type,

        @PositiveOrZero(message = "A capacidade máxima não pode ser negativa.")
        @Schema(description = "Número máximo de inscrições ativas. Null mantém o valor atual neste PATCH; use PUT /api/events/activities/{id}/capacity com null para remover o limite.", example = "40")
        Integer maxCapacity
) {
    @AssertTrue(message = "A data de início não pode ser posterior à data de término")
    public boolean isStartDateBeforeEndDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}
