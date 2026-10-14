package com.ccomp.br.domain.events.certificate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveCertificateBoxesRequest(
        @NotEmpty(message = "A lista de caixas não pode estar vazia.")
        @Size(max = 30, message = "O template aceita no máximo 30 caixas de texto.")
        @Valid
        List<BoxElementDTO> boxes
) {
}
