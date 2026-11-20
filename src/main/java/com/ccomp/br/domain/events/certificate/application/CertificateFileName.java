package com.ccomp.br.domain.events.certificate.application;

import java.text.Normalizer;
import java.util.Locale;

public final class CertificateFileName {
    private CertificateFileName() {
    }

    public static String create(String... names) {
        StringBuilder filename = new StringBuilder("certificado");
        if (names != null) {
            for (String name : names) {
                appendFirstTwoWords(filename, name);
            }
        }
        return filename.append(".png").toString();
    }

    private static void appendFirstTwoWords(StringBuilder filename, String name) {
        if (name == null || name.isBlank()) return;

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String[] words = normalized.split("[^a-z0-9]+");
        int appended = 0;
        for (String word : words) {
            if (word.isEmpty()) continue;
            filename.append('-').append(word);
            if (++appended == 2) break;
        }
    }
}
