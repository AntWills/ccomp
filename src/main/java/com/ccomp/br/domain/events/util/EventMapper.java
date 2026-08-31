package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.events.EventDTO;
import com.ccomp.br.domain.events.dto.events.UpdateEventDTO;
import com.ccomp.br.domain.events.persistence.Event;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface EventMapper {
//    EventListItem eventToEventResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "title", ignore = true)
    void updateEntityFromDto(UpdateEventDTO dto, @MappingTarget Event entity);

    EventDTO eventToEventDTO(Event event);
}
