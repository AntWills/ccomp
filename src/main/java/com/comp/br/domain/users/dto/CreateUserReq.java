package com.comp.br.domain.users.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserReq(
    @NotBlank(message = "Name is required.")
    @Size(min = 4, max = 255, message = "Name must between 4 and 255 characters")
    String name
) {
}
