package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar um ministrante")
public record AddInstructorRequest(
        @Schema(description = "Nome público do ministrante", example = "Prof. João Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Descrição pública do ministrante")
        @Size(max = 5000) String description,
        @Schema(description = "E-mail opcional de uma conta ativa para vincular ao ministrante", example = "joao@example.com")
        @Email @Size(max = 255) String accountEmail,
        @Schema(description = "Função na atividade; padrão INSTRUCTOR", example = "INSTRUCTOR")
        EnumInstructorRole role
) {
}
