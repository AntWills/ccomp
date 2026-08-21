package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.EventDTO;
import com.ccomp.br.domain.events.dto.UpdateEventRequest;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.shared.dto.EventListItem;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface EventMapper {
    com.ccomp.br.shared.dto.EventListItem eventToEventResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "title", ignore = true)
    void updateEntityFromDto(UpdateEventRequest dto, @MappingTarget Event entity);

    EventDTO eventToEventDTO(Event event);

    EventListItem eventToEventListItem(Event event);
}
