package com.ccomp.br.domain.events.materials.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMaterialLinkRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 2048) String url
) {}
