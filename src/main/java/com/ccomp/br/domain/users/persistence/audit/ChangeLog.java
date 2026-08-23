package com.ccomp.br.domain.users.persistence.audit;

public record ChangeLog (
        Object oldValue,
        Object newValue
) {
}
