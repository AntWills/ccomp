package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar um ministrante")
public record AddInstructorRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Email @Size(max = 255) String accountEmail,
        EnumInstructorRole role
) {
}
