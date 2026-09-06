package com.ccomp.br.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CursorUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private CursorUtils() {
    }

    public static <K> @Nullable String encode(@Nullable K cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String json = MAPPER.writeValueAsString(cursor);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Erro ao codificar cursor", e);
        }
    }

    public static <K> @Nullable K decode(String encoded, Class<K> type) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cursor inválido ou corrompido", e);
        }
    }

    public static <T, C> @NonNull CursorPage<T> buildPage(
            @NonNull List<T> results, int pageSize, @NonNull Function<T, C> cursorExtractor) {

        boolean hasNext = results.size() > pageSize;
        List<T> page = hasNext ? results.subList(0, pageSize) : results;

        String nextCursor = (hasNext && !page.isEmpty())
                ? encode(cursorExtractor.apply(page.getLast()))
                : null;

        return new CursorPage<>(page, nextCursor, null);
    }
}
