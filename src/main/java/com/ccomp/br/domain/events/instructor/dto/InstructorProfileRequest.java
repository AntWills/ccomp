package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstructorProfileRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Email @Size(max = 255) String accountEmail
) {}
