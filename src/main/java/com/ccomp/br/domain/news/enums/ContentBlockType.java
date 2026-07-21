package com.ccomp.br.domain.news.enums;

import com.fasterxml.jackson.annotation.JsonValue;

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
