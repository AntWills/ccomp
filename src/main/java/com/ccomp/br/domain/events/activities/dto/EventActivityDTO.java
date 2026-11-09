package com.ccomp.br.domain.events.activities.dto;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventActivityDTO(
        Long id,
        Long eventId,
        String title,
        String description,
        Long displayOrder,
        String location,
        @Schema(name = "start_date", description = "Horário local no fuso IANA retornado em time_zone.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário local no fuso IANA retornado em time_zone.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        BigDecimal workloadHours,
        EnumActivityRegistrationPolicy registrationPolicy,
        EnumActivityType type,
        LocalDateTime createdAt,
        @Schema(name = "time_zone", description = "Fuso global da aplicação; mantido por compatibilidade.", example = "America/Sao_Paulo", deprecated = true)
        String timeZone,
        Integer maxCapacity
) {
    public EventActivityDTO(Long id, Long eventId, String title, String description, Long displayOrder,
                            String location, LocalDateTime startDate, LocalDateTime endDate,
                            BigDecimal workloadHours, EnumActivityRegistrationPolicy registrationPolicy,
                            EnumActivityType type, LocalDateTime createdAt) {
        this(id, eventId, title, description, displayOrder, location, startDate, endDate, workloadHours,
                registrationPolicy, type, createdAt, "America/Sao_Paulo", null);
    }
}
