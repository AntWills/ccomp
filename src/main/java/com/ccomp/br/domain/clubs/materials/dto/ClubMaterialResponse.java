package com.ccomp.br.domain.clubs.materials.dto;

import com.ccomp.br.domain.clubs.materials.enums.EnumClubMaterialType;

import java.time.LocalDateTime;

public record ClubMaterialResponse(
        Long id,
        String title,
        String description,
        EnumClubMaterialType type,
        String url,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        LocalDateTime createdAt
) {}
