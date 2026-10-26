package com.ccomp.br.domain.observability.enums;

import java.util.Arrays;
import java.util.Locale;

public enum InfraLogServiceName {
    PROXY("proxy"),
    CCOMP_BACKEND("ccomp-backend"),
    POSTGRES_DB("postgres-db"),
    POSTGRES_EXPORTER("postgres-exporter"),
    SEAWEEDS("seaweeds"),
    SEAWEEDS_SETUP("seaweeds-setup"),
    RABBITMQ("rabbitmq"),
    REDIS("redis"),
    REDIS_EXPORTER("redis-exporter"),
    GRAFANA_LGTM("grafana-lgtm"),
    PROMETHEUS("prometheus"),
    ALLOY("alloy");

    private final String value;

    InfraLogServiceName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static InfraLogServiceName fromValue(String value) {
        return Arrays.stream(values())
                .filter(service -> service.value.equals(value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Serviço de infraestrutura desconhecido."));
    }
}
