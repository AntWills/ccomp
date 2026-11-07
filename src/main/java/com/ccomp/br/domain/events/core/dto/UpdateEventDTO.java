package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumScheduleConflictPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime enrollmentStartDate,
        LocalDateTime enrollmentEndDate,
        Boolean enrollmentPaused,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária base do evento, somada às atividades com presença ao emitir certificado de participante.", example = "2.5")
        BigDecimal workloadHours,
        @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Carga horária concedida aos certificados do owner e dos editores ativos.", example = "8")
        BigDecimal teamCertificateWorkloadHours,
        @Size(max = 255, message = "A organização deve ter no máximo 255 caracteres.")
        @Schema(description = "Organização responsável pelo evento. Opcional.", example = "Instituto Federal do Maranhão")
        String organization
) {
    public Optional<String> titleOpt() { return Optional.ofNullable(title); }
}
