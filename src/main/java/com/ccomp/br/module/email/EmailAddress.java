package com.ccomp.br.module.email;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Locale;
import java.util.Objects;

@Embeddable
public class EmailAddress {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String value;

    protected EmailAddress() {
        // Required by JPA
    }

    @JsonCreator
    public EmailAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email não pode ser null ou em branco.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if(!EmailValidator.getInstance().isValid(normalized))
            throw new IllegalArgumentException("Formato de e-mail inválido: " + value);

        this.value = normalized;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailAddress email)) return false;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
