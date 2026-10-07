package com.ccomp.br.domain.events.certificate.dto;

import com.ccomp.br.module.certificate.Certificate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record GenerateCertificatePreviewRequest(
        @NotEmpty(message = "A lista de elementos de texto não pode estar vazia.")
        @Valid
        List<TextElementDTO> textElements,

        Map<String, String> variables
) {
    public record TextElementDTO(
            @NotNull(message = "O template de texto é obrigatório.")
            String textTemplate,

            @NotNull(message = "A coordenada X é obrigatória.")
            Double x,

            @NotNull(message = "A coordenada Y é obrigatória.")
            Double y,

            Integer fontSize,
            String hexColor
    ) {
        public Certificate.TextElement toDomain() {
            return new Certificate.TextElement(textTemplate, x, y, fontSize, hexColor);
        }
    }
}
