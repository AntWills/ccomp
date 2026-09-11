package com.ccomp.br.domain.events.dto.activities;

import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Builder;

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
