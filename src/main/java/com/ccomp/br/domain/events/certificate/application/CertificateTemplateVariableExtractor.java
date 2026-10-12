package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.shared.exceptions.DomainException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CertificateTemplateVariableExtractor {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^{}]+}");
    private static final Set<String> ALLOWED_VARIABLES = Set.copyOf(CertificateVariablesServices.VARIABLES);

    private CertificateTemplateVariableExtractor() {
    }

    static List<String> extract(List<BoxElementDTO> boxes) {
        Set<String> extractedVariables = new LinkedHashSet<>();

        for (BoxElementDTO box : boxes) {
            Matcher matcher = VARIABLE_PATTERN.matcher(box.textTemplate());
            while (matcher.find()) {
                String variable = matcher.group();
                if (!ALLOWED_VARIABLES.contains(variable)) {
                    throw new DomainException("Variável não suportada no template: " + variable);
                }
                extractedVariables.add(variable);
            }
        }

        return List.copyOf(extractedVariables);
    }
}
