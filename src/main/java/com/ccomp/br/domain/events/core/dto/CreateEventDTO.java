package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
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
        @Schema(name = "start_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária base configurada no evento. Na emissão do certificado do participante, soma-se às cargas das atividades em que ele teve presença.", example = "2.5")
        BigDecimal workloadHours,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Opcional na criação; precisa estar configurada para emitir certificados do owner e dos editores ativos.", example = "8")
        BigDecimal teamCertificateWorkloadHours,
        @Size(max = 255, message = "A organização deve ter no máximo 255 caracteres.")
        @Schema(description = "Organização responsável pelo evento. Opcional.", example = "Instituto Federal do Maranhão")
        String organization,
        @PositiveOrZero(message = "A capacidade máxima não pode ser negativa.")
        @Schema(description = "Número máximo de inscrições ativas. Null significa sem limite; se omitido, inicia sem limite.", example = "150")
        Integer maxCapacity,
        @NotBlank(message = "O fuso horário do evento é obrigatório.")
        @Schema(name = "time_zone", description = "DEPRECATED. Ainda obrigatório e persistido por compatibilidade, mas não define o fuso dos horários. Use Brasília (America/Sao_Paulo).", example = "America/Sao_Paulo", requiredMode = Schema.RequiredMode.REQUIRED, deprecated = true)
        String timeZone
) {
    public CreateEventDTO(String title, EnumEventCategory category, EnumEventFormat format,
                          LocalDateTime startDate, LocalDateTime endDate) {
        this(title, category, format, startDate, endDate, null, null, null, null, "America/Sao_Paulo");
    }

    public Optional<LocalDateTime> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optionalEndDate() {
        return Optional.ofNullable(endDate);
    }
}
