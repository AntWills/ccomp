package com.ccomp.br.domain.events.activities.dto;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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

        EnumActivityType type
) {
    @AssertTrue(message = "A data de início não pode ser posterior à data de término")
    public boolean isStartDateBeforeEndDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}
