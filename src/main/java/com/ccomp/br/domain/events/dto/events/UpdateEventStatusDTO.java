package com.ccomp.br.domain.events.dto.events;

import com.ccomp.br.domain.events.enums.EnumEventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusDTO(
        @NotNull(message = "O novo status do evento é obrigatório.")
        EnumEventStatus status
) {
}
