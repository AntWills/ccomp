package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.dto.EventListItem;
import com.ccomp.br.domain.events.dto.UpdateEventRequest;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.shared.dto.EventResponse;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface EventMapper {
    EventResponse eventToEventResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "title", ignore = true)
    void updateEntityFromDto(UpdateEventRequest dto, @MappingTarget Event entity);

    EventListItem eventToEventListItem(Event event);
}
