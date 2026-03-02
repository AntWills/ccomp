package com.ccomp.br.domain.events.util;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.shared.dto.EventResponse;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventResponse eventToEventResponse(Event event);
}
