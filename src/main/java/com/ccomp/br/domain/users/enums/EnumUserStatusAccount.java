package com.ccomp.br.domain.users.enums;

import com.ccomp.br.shared.exceptions.DomainException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumUserStatusAccount {
    ACTIVE, DEACTIVATED, BLOCKED;

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static EnumUserStatusAccount fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (EnumUserStatusAccount category : EnumUserStatusAccount.values()) {
            if (category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }

        throw new DomainException("O status da conta é inválida: " + value + ".");
    }
}
