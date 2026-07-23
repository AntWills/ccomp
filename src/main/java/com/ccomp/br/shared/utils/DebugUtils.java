package com.ccomp.br.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class DebugUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static String printJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            return  "Erro ao converter entidade para JSON: " + e.getMessage();
        }
    }
}
