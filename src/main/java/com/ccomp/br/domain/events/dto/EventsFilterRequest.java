package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;

import java.util.Optional;

public record EventsFilterRequest(
        EnumEventCategory eventCategory,
        EnumEventFormat format
) {
    public Optional<EnumEventCategory> optionalEnumEventCategory() {
        return Optional.ofNullable(eventCategory);
    }
}
