package com.ccomp.br.domain.observability.enums;

import java.util.Arrays;
import java.util.Locale;

public enum InfraLogLevel {
    INFO,
    WARN,
    ERROR,
    FATAL,
    PANIC,
    WARNING,
    LOG,
    NOTICE,
    DEBUG1,
    DEBUG2,
    DEBUG3,
    DEBUG4,
    DEBUG5;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static InfraLogLevel fromValue(String value) {
        return Arrays.stream(values())
                .filter(level -> level.value().equals(value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nível de log desconhecido."));
    }
}
