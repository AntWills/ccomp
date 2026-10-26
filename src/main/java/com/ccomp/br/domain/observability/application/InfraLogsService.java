package com.ccomp.br.domain.observability.application;

import com.ccomp.br.domain.observability.dto.InfraLogEntry;
import com.ccomp.br.domain.observability.dto.InfraLogsResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class InfraLogsService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;
    private static final Duration MAX_RANGE = Duration.ofDays(7);
    private static final Pattern LABEL_VALUE = Pattern.compile("[a-zA-Z0-9_.-]{1,128}");
    private static final BigInteger NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L);

    private final RestClient loki;

    public InfraLogsService(RestClient lokiRestClient) {
        this.loki = lokiRestClient;
    }

    public InfraLogsResponse search(
            String serviceName,
            String level,
            String search,
            Instant requestedStart,
            Instant requestedEnd,
            Integer requestedLimit,
            String cursor
    ) {
        CursorWindow cursorWindow = decodeCursor(cursor);
        Instant end = cursorWindow == null
                ? (requestedEnd == null ? Instant.now() : requestedEnd)
                : cursorWindow.end();
        Instant start = cursorWindow == null
                ? (requestedStart == null ? end.minus(Duration.ofHours(1)) : requestedStart)
                : cursorWindow.start();
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;

        validateWindow(start, end);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + MAX_LIMIT + ".");
        }
        validateLabel("serviceName", serviceName);
        validateLabel("level", level);
        if (search != null && search.length() > 256) {
            throw new IllegalArgumentException("O texto de busca deve ter no máximo 256 caracteres.");
        }

        String query = buildQuery(serviceName, level, search);
        Map<?, ?> response = loki.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/loki/api/v1/query_range")
                        // O placeholder impede que as chaves do seletor LogQL sejam lidas como
                        // template de URI; o valor expandido é codificado pelo UriBuilder uma vez.
                        .queryParam("query", "{query}")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .queryParam("direction", "backward")
                        .queryParam("limit", limit + 1)
                        .build(query))
                .retrieve()
                .body(Map.class);

        List<InfraLogEntry> logs = flatten(response);
        logs.sort(Comparator.comparing(InfraLogEntry::timestamp).reversed());
        boolean hasMore = logs.size() > limit;
        if (hasMore) {
            logs = new ArrayList<>(logs.subList(0, limit));
        }
        String nextCursor = null;
        if (hasMore && !logs.isEmpty()) {
            Instant nextEnd = logs.getLast().timestamp().minusNanos(1);
            if (!nextEnd.isBefore(start)) {
                nextCursor = encodeCursor(start, nextEnd);
            }
        }
        return new InfraLogsResponse(logs, start, end, limit, nextCursor);
    }

    private static void validateWindow(Instant start, Instant end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("O início do período deve ser anterior ao fim.");
        }
        if (Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("O período máximo consultável é de 7 dias.");
        }
    }

    private static void validateLabel(String name, String value) {
        if (value != null && !LABEL_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Filtro inválido: " + name + ".");
        }
    }

    private static String buildQuery(String serviceName, String level, String search) {
        List<String> selectors = new ArrayList<>();
        selectors.add("job=\"ccomp\"");
        String effectiveService = serviceName == null || serviceName.isBlank() ? "ccomp-backend" : serviceName;
        selectors.add("service_name=\"" + effectiveService + "\"");
        if (level != null && !level.isBlank()) {
            selectors.add("level=\"" + level + "\"");
        }

        String query = "{" + String.join(",", selectors) + "}";
        if (search != null && !search.isBlank()) {
            query += " |= \"" + escapeLogQLString(search) + "\"";
        }
        return query;
    }

    private static String encodeCursor(Instant start, Instant end) {
        String value = start.toString() + "\n" + end.toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static CursorWindow decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] bounds = value.split("\\n", -1);
            if (bounds.length != 2) {
                throw new IllegalArgumentException();
            }
            return new CursorWindow(Instant.parse(bounds[0]), Instant.parse(bounds[1]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Cursor de navegação inválido.");
        }
    }

    private record CursorWindow(Instant start, Instant end) {
    }

    private static String escapeLogQLString(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static List<InfraLogEntry> flatten(Map<?, ?> response) {
        List<InfraLogEntry> logs = new ArrayList<>();
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)
                || !(data.get("result") instanceof List<?> result)) {
            return logs;
        }

        for (Object item : result) {
            if (!(item instanceof Map<?, ?> streamResult)) {
                continue;
            }
            Map<?, ?> labels = streamResult.get("stream") instanceof Map<?, ?> stream ? stream : Map.of();
            if (!(streamResult.get("values") instanceof List<?> values)) {
                continue;
            }
            for (Object itemValue : values) {
                if (!(itemValue instanceof List<?> value) || value.size() < 2) {
                    continue;
                }
                logs.add(new InfraLogEntry(
                        parseNanosecondTimestamp(String.valueOf(value.get(0))),
                        label(labels, "service_name"),
                        label(labels, "level"),
                        String.valueOf(value.get(1))
                ));
            }
        }
        return logs;
    }

    private static String label(Map<?, ?> labels, String name) {
        Object value = labels.get(name);
        return value == null ? null : value.toString();
    }

    private static Instant parseNanosecondTimestamp(String timestamp) {
        BigInteger nanos = new BigInteger(timestamp);
        BigInteger[] parts = nanos.divideAndRemainder(NANOS_PER_SECOND);
        if (parts[1].signum() < 0) {
            parts[0] = parts[0].subtract(BigInteger.ONE);
            parts[1] = parts[1].add(NANOS_PER_SECOND);
        }
        return Instant.ofEpochSecond(parts[0].longValueExact(), parts[1].longValueExact());
    }
}
