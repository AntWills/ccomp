package com.ccomp.br.domain.audit.persistence;

public record ChangeLog (
        Object oldValue,
        Object newValue
) {
}
