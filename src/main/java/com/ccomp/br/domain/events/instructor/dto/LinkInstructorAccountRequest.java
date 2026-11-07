package com.ccomp.br.domain.events.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LinkInstructorAccountRequest(@NotBlank @Email String email) {
}
