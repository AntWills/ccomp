package com.ccomp.br.domain.events.activities.utils;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ActivityMapper {
    @Mapping(source = "event.id", target = "eventId")
    ActivityDTO eventActivityToActivityDTO(EventActivity eventActivity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEventActivityFromRequest(UpdateActivityDTO request, @MappingTarget EventActivity activity);
}
