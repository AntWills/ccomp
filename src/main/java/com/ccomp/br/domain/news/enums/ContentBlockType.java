package com.ccomp.br.domain.news.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipos de blocos de conteúdo suportados")
public enum ContentBlockType {
    HEADING, PARAGRAPH, IMAGE, QUOTE, SOCIAL_LINK;

    @Override
    public String toString() {
        return name();
    }

    @JsonValue
    public String toJson() {
        return toString();
    }
}
