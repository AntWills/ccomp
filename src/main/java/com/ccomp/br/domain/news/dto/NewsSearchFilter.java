package com.ccomp.br.domain.news.dto;

import java.util.Optional;

public record NewsSearchFilter(
        Boolean featured
) {
    public Optional<Boolean> featuredOpt() {
        return Optional.ofNullable(featured);
    }
}
