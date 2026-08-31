package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.activities.ActivityDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.dto.activities.UpdateActivityDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ActivityMapper {
    ActivityDTO eventActivityToActivityDTO(EventActivity eventActivity);

    void updateEventActivityFromRequest(UpdateActivityDTO request, @MappingTarget EventActivity activity);
}
