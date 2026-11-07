package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conta ativa a associar ao registro de ministrante")
public record LinkInstructorAccountRequest(
        @Schema(description = "E-mail da conta ativa", example = "joao@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email
) {
}
