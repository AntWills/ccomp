package com.ccomp.br.domain.events.certificate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record GenerateCertificatePreviewRequest(
        @NotEmpty(message = "A lista de elementos de texto não pode estar vazia.")
        @Size(max = 30, message = "A prévia aceita no máximo 30 caixas de texto.")
        @Valid
        List<BoxElementDTO> boxElements,

        Map<String, String> variables
) {
}