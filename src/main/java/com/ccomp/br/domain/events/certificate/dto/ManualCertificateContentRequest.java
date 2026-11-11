package com.ccomp.br.domain.events.certificate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** Certificate text boxes and replacement values supplied by the event manager. */
public record ManualCertificateContentRequest(
        @Schema(description = "Caixas de texto enviadas pelo cliente, com texto, posição, largura e estilo.")
        @NotNull
        @Size(min = 1, max = 30, message = "Informe de 1 a 30 caixas de texto.")
        List<@Valid BoxElementDTO> boxes,

        @Schema(description = "Substituições de texto. Use como chave o marcador presente em textTemplate, por exemplo {participante.nome}.")
        @NotNull
        @Size(max = 50, message = "Informe no máximo 50 variáveis para o certificado.")
        Map<@NotBlank @Size(max = 100) String, @NotNull @Size(max = 2000) String> variables
) {
}
