package com.ccomp.br.domain.events.certificate.application.variables;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateVariableContext;
import com.ccomp.br.shared.exceptions.DomainException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class CertificateTemplateVariableExtractor {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^{}]+}");

    private CertificateTemplateVariableExtractor() {
    }

    public static List<String> extract(List<BoxElementDTO> boxes, EnumCertificateVariableContext context) {
        Set<String> allowedVariables = Set.copyOf(CertificateVariablesServices.acceptedVariablesFor(context));
        Set<String> extractedVariables = new LinkedHashSet<>();

        for (BoxElementDTO box : boxes) {
            Matcher matcher = VARIABLE_PATTERN.matcher(box.textTemplate());
            while (matcher.find()) {
                String variable = matcher.group();
                if (!allowedVariables.contains(variable)) {
                    throw new DomainException("Variável não suportada para certificado " + context + ": " + variable);
                }
                extractedVariables.add(variable);
            }
        }

        return List.copyOf(extractedVariables);
    }
}
