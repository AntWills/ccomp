package com.ccomp.br.domain.audit.external.dto;

public record ChangeLog (
        Object oldValue,
        Object newValue
) {
}
