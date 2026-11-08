package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumScheduleConflictPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public record UpdateEventDTO(
        @Size(min = 4, max = 255, message = "O titulo deve ter entre 4 e 255 caracteres.")
        String title,

        @Size(min = 4, max = 255, message = "A descrição deve ter entre 4 e 255 caracteres.")
        String summary,

        @Size(min = 4, max = 5000, message = "A descrição deve ter entre 4 e 5000 caracteres.")
        String content,

        EnumEventCategory category,
        EnumEventFormat format,
        EnumScheduleConflictPolicy scheduleConflictPolicy,
        @Schema(name = "start_date", description = "Horário local de início no fuso time_zone do evento.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário local de término no fuso time_zone do evento.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        @Schema(name = "enrollment_start_date", description = "Início local das inscrições no fuso time_zone do evento.", example = "2026-10-01T09:00:00")
        LocalDateTime enrollmentStartDate,
        @Schema(name = "enrollment_end_date", description = "Fim local das inscrições no fuso time_zone do evento.", example = "2026-11-08T12:00:00")
        LocalDateTime enrollmentEndDate,
        Boolean enrollmentPaused,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária base configurada no evento. Na emissão do certificado do participante, soma-se às cargas das atividades em que ele teve presença.", example = "2.5")
        BigDecimal workloadHours,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária concedida aos certificados do owner e dos editores ativos; precisa estar configurada para emitir esses certificados.", example = "8")
        BigDecimal teamCertificateWorkloadHours,
        @Size(max = 255, message = "A organização deve ter no máximo 255 caracteres.")
        @Schema(description = "Organização responsável pelo evento. Opcional.", example = "Instituto Federal do Maranhão")
        String organization,
        @PositiveOrZero(message = "A capacidade máxima não pode ser negativa.")
        @Schema(description = "Número máximo de inscrições ativas. Null mantém o valor atual neste PATCH; use PUT /api/events/{eventId}/capacity com null para remover o limite.", example = "150")
        Integer maxCapacity,
        @Schema(name = "time_zone", description = "Opcional. Novo identificador IANA para interpretar os horários locais do evento. Se omitido ou null, mantém o fuso atual.", example = "America/Manaus")
        String timeZone
) {
    public Optional<String> titleOpt() { return Optional.ofNullable(title); }
}
