package com.ccomp.br.domain.events.dto;

import com.ccomp.br.domain.events.enums.EnumEventCategory;

import java.util.Optional;

public record EventsFilterRequest(
        EnumEventCategory eventCategory
) {
    public Optional<EnumEventCategory> optionalEnumEventCategory() {
        return Optional.ofNullable(eventCategory);
    }
}
