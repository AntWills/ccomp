package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusDTO(
        @NotNull(message = "O novo status do evento é obrigatório.")
        EnumEventStatus status
) {
}
