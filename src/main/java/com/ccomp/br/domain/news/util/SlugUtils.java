package com.ccomp.br.domain.news.util;

import com.ccomp.br.shared.exceptions.DomainException;

import java.text.Normalizer;

public class SlugUtils {
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) throw new DomainException("Não foi possível criar o slug da notícia.");

        String simplified = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        simplified = simplified.replace("&", "e");

        String base = simplified.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if(base.isEmpty()) throw new DomainException("Não foi possível criar o slug da notícia.");

        // 4. Sufixo opcional (Idealmente apenas se houver colisão no DB)
        return base;
    }
}
