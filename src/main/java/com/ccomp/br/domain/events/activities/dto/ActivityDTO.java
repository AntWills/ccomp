package com.ccomp.br.domain.events.activities.dto;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityDTO(
        Long id,
        Long eventId,
        String title,
        String description,
        Long displayOrder,
        String location,
        @Schema(name = "start_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        BigDecimal workloadHours,
        EnumActivityRegistrationPolicy registrationPolicy,
        EnumActivityType type,
        @Schema(name = "time_zone", description = "DEPRECATED. Retornado por compatibilidade; horários de atividades usam Brasília (America/Sao_Paulo).", example = "America/Sao_Paulo", deprecated = true)
        String timeZone
) {
    public ActivityDTO(Long id, Long eventId, String title, String description, Long displayOrder,
                       String location, LocalDateTime startDate, LocalDateTime endDate,
                       BigDecimal workloadHours, EnumActivityRegistrationPolicy registrationPolicy,
                       EnumActivityType type) {
        this(id, eventId, title, description, displayOrder, location, startDate, endDate, workloadHours,
                registrationPolicy, type, "America/Sao_Paulo");
    }
}
