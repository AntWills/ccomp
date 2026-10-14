package com.ccomp.br.domain.auth.core.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "A credencial Google é obrigatória.")
        String credential
) {
}
