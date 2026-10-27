package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;

import java.util.Optional;

public record EventsFilterRequest(
        EnumEventCategory category,
        EnumEventFormat format,
        String searchTerm
) {
    public EventsFilterRequest(EnumEventCategory category, EnumEventFormat format) {
        this(category, format, null);
    }

    public Optional<EnumEventCategory> categoryOpt() {
        return Optional.ofNullable(category);
    }
    public Optional<EnumEventFormat> formatOpt() { return Optional.ofNullable(format); }
    public Optional<String> searchTermOpt() {
        return Optional.ofNullable(searchTerm).map(String::trim).filter(term -> !term.isBlank());
    }
}
