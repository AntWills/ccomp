package com.ccomp.br.domain.news.utils;

import com.ccomp.br.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlugUtilsTest {
    @Test
    void stripsAccentsNormalizesCaseAndSeparators() {
        assertThat(SlugUtils.toSlug("  Notícias & Eventos: São João! "))
                .isEqualTo("noticias-e-eventos-sao-joao");
    }

    @Test
    void rejectsBlankAndNonSluggableInputs() {
        assertThrows(DomainException.class, () -> SlugUtils.toSlug("  "));
        assertThrows(DomainException.class, () -> SlugUtils.toSlug("💙"));
    }
}
