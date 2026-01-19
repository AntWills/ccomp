package com.comp.br.modules.users.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserReq(
    @NotBlank(message = "Name is required.") String name
) {
}
