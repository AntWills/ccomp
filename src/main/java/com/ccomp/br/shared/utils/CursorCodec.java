package com.ccomp.br.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public final class CursorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private CursorCodec() {
    }

    public static <K> String encode(K cursor) {
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

    public static <K> Optional<K> decode(String encoded, Class<K> type) {
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            return Optional.of(MAPPER.readValue(json, type));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cursor inválido ou corrompido", e);
        }
    }
}
