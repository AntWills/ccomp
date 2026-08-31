package com.ccomp.br.domain.events.dto.events;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;

import java.util.Optional;

public record EventsFilterRequest(
        EnumEventCategory category,
        EnumEventFormat format
) {
    public Optional<EnumEventCategory> categoryOpt() {
        return Optional.ofNullable(category);
    }
    public Optional<EnumEventFormat> formatOpt() { return Optional.ofNullable(format); }
}
