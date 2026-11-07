package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados necessários para convidar um ministrante")
public record InviteInstructorRequest(
        @Schema(description = "E-mail da pessoa convidada", example = "ministrante@exemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email
) {
}
