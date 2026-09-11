package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.activities.ActivityDTO;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.dto.activities.UpdateActivityDTO;
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
