package com.ccomp.br.domain.users.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockAccountReq (
        @NotBlank
        String reason
) {
}
