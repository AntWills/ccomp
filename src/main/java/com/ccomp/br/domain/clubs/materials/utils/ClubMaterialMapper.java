package com.ccomp.br.domain.clubs.materials.utils;

import com.ccomp.br.domain.clubs.materials.dto.ClubMaterialResponse;
import com.ccomp.br.domain.clubs.materials.enums.EnumClubMaterialType;
import com.ccomp.br.domain.clubs.materials.persistence.ClubMaterial;
import org.springframework.stereotype.Component;

@Component
public class ClubMaterialMapper {
    public ClubMaterialResponse toDTO(ClubMaterial m) {
        return new ClubMaterialResponse(
                m.getId(), m.getTitle(), m.getDescription(), m.getType(),
                m.getType() == EnumClubMaterialType.LINK ? m.getUrl() : null,
                m.getOriginalFileName(), m.getContentType(), m.getSizeBytes(), m.getCreatedAt());
    }
}
