package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        String coverImageUrl,
        EnumEventCategory category,
        EnumEventFormat format,

        // Visibilidade e Publicação
        EnumEventStatus status,

        // Execução do Evento
        LocalDateTime startDate,
        LocalDateTime endDate,
        EnumEventExecutionStatus executionStatus,

        // Inscrições e Capacidade
        LocalDateTime enrollmentStartDate,
        LocalDateTime enrollmentEndDate,
        Boolean enrollmentPaused,
        EnumEnrollmentStatus enrollmentStatus,

        // Metadados
        UUID ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
