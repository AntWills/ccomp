package com.ccomp.br.domain.events.materials.utils;

import com.ccomp.br.domain.events.materials.dto.MaterialResponse;
import com.ccomp.br.domain.events.materials.persistence.EventMaterial;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterial;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {
    public MaterialResponse toDTO(EventMaterial m) {
        return new MaterialResponse(m.getId(), m.getTitle(), m.getDescription(), m.getType(),
                m.getType() == EnumMaterialType.LINK ? m.getUrl() : null, m.getOriginalFileName(), m.getContentType(), m.getSizeBytes(), m.getCreatedAt());
    }
    public MaterialResponse toDTO(ActivityMaterial m) {
        return new MaterialResponse(m.getId(), m.getTitle(), m.getDescription(), m.getType(),
                m.getType() == EnumMaterialType.LINK ? m.getUrl() : null, m.getOriginalFileName(), m.getContentType(), m.getSizeBytes(), m.getCreatedAt());
    }
}
