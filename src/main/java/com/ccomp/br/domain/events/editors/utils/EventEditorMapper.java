package com.ccomp.br.domain.events.editors.utils;

import com.ccomp.br.domain.events.editors.dto.EventEditorDTO;
import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface EventEditorMapper {
    @Mapping(target = "eventId", source = "event.id")
    EventEditorDTO eventEditorToEventEditorDTO(EventEditor eventEditor);
}
