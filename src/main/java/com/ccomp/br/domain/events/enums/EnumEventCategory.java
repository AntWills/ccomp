package com.ccomp.br.domain.events.enums;

import com.ccomp.br.shared.exceptions.DomainException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumEventCategory {
    ACADEMIC_EDUCATIONAL,
    CULTURE_ENTERTAINMENT,
    CORPORATE_BUSINESS,
    SOCIAL_POPULAR,
    SPORTS_WELLNESS,
    FOOD_DRINK,
    OTHER;

    @Override
    public String toString() {
        return name();
    }

    @JsonValue
    public String toJson() {
        return toString();
    }

    @JsonCreator
    public static EnumEventCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (EnumEventCategory category : EnumEventCategory.values()) {
            if (category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }

        throw new DomainException("A categoria do evento é inválida: " + value + ".");
    }
}
