package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Size;

public record UpdateInstructorProfileRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 5000) String description
) {}
