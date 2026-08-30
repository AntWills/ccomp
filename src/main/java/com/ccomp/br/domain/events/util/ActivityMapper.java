package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.dto.UpdateActivityRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ActivityMapper {
    ActivityDTO eventActivityToActivityDTO(EventActivity eventActivity);

    void updateEventActivityFromRequest(UpdateActivityRequest request, @MappingTarget EventActivity activity);
}
