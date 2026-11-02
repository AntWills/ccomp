package com.ccomp.br.domain.events.materials.dto;

import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;

import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        String title,
        String description,
        EnumMaterialType type,
        String url,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        LocalDateTime createdAt
) {}
