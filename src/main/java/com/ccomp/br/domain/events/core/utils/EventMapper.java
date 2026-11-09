package com.ccomp.br.domain.events.core.utils;

import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.dto.UpdateEventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import org.mapstruct.*;

import java.time.LocalDateTime;

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
    @Mapping(target = "timeZone", ignore = true)
    void updateEntityFromDto(UpdateEventDTO dto, @MappingTarget Event entity);

    @Mapping(target = "executionStatus", expression = "java(event.getExecutionStatus(now))")
    @Mapping(target = "enrollmentStatus", expression = "java(event.getEnrollmentStatus(now))")
    EventDTO eventToEventDTO(Event event, LocalDateTime now);

    default EventDTO eventToEventDTO(Event event) {
        return eventToEventDTO(event, LocalDateTime.now());
    }
}
