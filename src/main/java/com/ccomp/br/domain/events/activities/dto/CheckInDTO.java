package com.ccomp.br.domain.events.activities.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados para confirmar presença em uma atividade")
public record CheckInDTO (
        @Schema(
                description = "Código de presença contido no QR Code",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        @NotNull(message = "O código é obrigatório.")
        UUID code
) {
}
