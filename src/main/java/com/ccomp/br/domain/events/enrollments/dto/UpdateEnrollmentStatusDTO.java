package com.ccomp.br.domain.events.enrollments.dto;

import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import jakarta.validation.constraints.NotNull;

public record UpdateEnrollmentStatusDTO(
        @NotNull(message = "O novo status da inscrição é obrigatório.")
        EnumEnrollmentState status
) {
}
