package com.ccomp.br.domain.events.enums;

import com.ccomp.br.shared.exceptions.DomainException;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum EnumEventFormat {
    IN_PERSON,
    HYBRID,
    ONLINE;

    @JsonCreator
    public static EnumEventFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (EnumEventFormat format : EnumEventFormat.values()) {
            if (format.name().equalsIgnoreCase(value.trim())) {
                return format;
            }
        }

        throw new DomainException("O formato do evento é inválida: " + value + ".");
    }
}
