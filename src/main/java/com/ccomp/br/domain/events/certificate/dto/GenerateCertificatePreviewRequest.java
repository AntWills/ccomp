package com.ccomp.br.domain.events.certificate.dto;

import com.ccomp.br.module.certificate.BoxElement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    public record BoxElementDTO(
            @NotBlank(message = "O template de texto é obrigatório.")
            @Size(max = 2000, message = "O texto da caixa excede o limite de 2000 caracteres.")
            String textTemplate,

            @NotNull(message = "A coordenada X é obrigatória.")
            @DecimalMin(value = "0.0", message = "X deve estar entre 0 e 1.")
            @DecimalMax(value = "1.0", message = "X deve estar entre 0 e 1.")
            Double x,

            @NotNull(message = "A coordenada Y é obrigatória.")
            @DecimalMin(value = "0.0", message = "Y deve estar entre 0 e 1.")
            @DecimalMax(value = "1.0", message = "Y deve estar entre 0 e 1.")
            Double y,

            @NotNull(message = "A largura da caixa é obrigatória.")
            @DecimalMin(value = "0.01", message = "A largura deve ser maior que zero.")
            @DecimalMax(value = "1.0", message = "A largura deve estar entre 0 e 1.")
            Double width,

            @NotNull(message = "A altura da caixa é obrigatória.")
            @DecimalMin(value = "0.01", message = "A altura deve ser maior que zero.")
            @DecimalMax(value = "1.0", message = "A altura deve estar entre 0 e 1.")
            Double height,

            @Min(value = 8, message = "O tamanho da fonte deve ser de no mínimo 8pt.")
            @Max(value = 300, message = "O tamanho da fonte deve ser de no máximo 300pt.")
            Float fontSize,

            @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$", message = "A cor da fonte deve ser um hexadecimal válido (ex: #000000).")
            String fontColor
    ) {
        public BoxElement toDomain() {
            return new BoxElement(textTemplate, x, y, width, height, fontSize, fontColor);
        }
    }
}