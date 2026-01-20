package com.comp.br.domain.users.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserReq(
    @NotBlank(message = "Name is required.") String name
) {
}
