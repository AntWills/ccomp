package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.ActivityDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper {
    ActivityDTO eventActivityToActivityDTO(EventActivity eventActivity);
}
