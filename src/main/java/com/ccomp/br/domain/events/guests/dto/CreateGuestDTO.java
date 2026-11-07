package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criar um perfil de convidado no evento")
public record CreateGuestDTO(
        @Schema(description = "Nome público do convidado", example = "Ana Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Descrição pública do convidado")
        @Size(max = 5000) String description,
        @Schema(description = "E-mail opcional de uma conta ativa para vincular ao perfil", example = "ana@example.com")
        @Email @Size(max = 255) String accountEmail
) {
    public CreateGuestDTO(String name, String description) {
        this(name, description, null);
    }
}
