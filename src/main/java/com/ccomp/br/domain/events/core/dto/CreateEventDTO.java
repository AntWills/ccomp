package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public record CreateEventDTO(
        @NotNull
        @Size(min = 4, max = 255, message = "O titulo deve ter entre 4 e 255 caracteres.")
        String title,

        @NotNull(message = "A categoria do evento é obrigatória.")
        EnumEventCategory category,
        @NotNull(message = "O formato do evento é obrigatório.")
        EnumEventFormat format,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária base do evento, somada às atividades com presença ao emitir certificado de participante.", example = "2.5")
        BigDecimal workloadHours,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária concedida aos certificados do owner e dos editores ativos. Deve ser definida pela equipe do evento.", example = "8")
        BigDecimal teamCertificateWorkloadHours,
        @Size(max = 255, message = "A organização deve ter no máximo 255 caracteres.")
        @Schema(description = "Organização responsável pelo evento. Opcional.", example = "Instituto Federal do Maranhão")
        String organization
) {
    public CreateEventDTO(String title, EnumEventCategory category, EnumEventFormat format,
                          LocalDateTime startDate, LocalDateTime endDate) {
        this(title, category, format, startDate, endDate, null, null, null);
    }

    public Optional<LocalDateTime> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optionalEndDate() {
        return Optional.ofNullable(endDate);
    }
}
