package com.ccomp.br.domain.events.core.dto;

import com.ccomp.br.domain.events.core.enums.*;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        String coverImageKey,
        EnumEventCategory category,
        EnumEventFormat format,
        EnumScheduleConflictPolicy scheduleConflictPolicy,

        // Visibilidade e Publicação
        EnumEventStatus status,

        // Execução do Evento
        @Schema(name = "start_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T09:05:00")
        LocalDateTime startDate,
        @Schema(name = "end_date", description = "Horário de Brasília (America/Sao_Paulo), sem offset.", example = "2026-11-08T12:00:00")
        LocalDateTime endDate,
        EnumEventExecutionStatus executionStatus,

        // Inscrições e Capacidade
        @Schema(name = "enrollment_start_date", description = "Início das inscrições em horário de Brasília (America/Sao_Paulo).", example = "2026-10-01T09:00:00")
        LocalDateTime enrollmentStartDate,
        @Schema(name = "enrollment_end_date", description = "Fim das inscrições em horário de Brasília (America/Sao_Paulo).", example = "2026-11-08T12:00:00")
        LocalDateTime enrollmentEndDate,
        Boolean enrollmentPaused,
        EnumEnrollmentStatus enrollmentStatus,

        // Metadados
        UUID ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // Endereço / local do evento (usado nos certificados em {evento.local})
        String address,
        @Schema(description = "Carga horária base configurada no evento. Na emissão do certificado do participante, soma-se às cargas das atividades em que ele teve presença.")
        BigDecimal workloadHours,
        @Schema(description = "Carga horária configurada para certificados do owner e de editores ativos.")
        BigDecimal teamCertificateWorkloadHours,
        @Schema(description = "Organização responsável pelo evento.")
        String organization,
        @Schema(name = "time_zone", description = "DEPRECATED. Retornado por compatibilidade; não altera a interpretação dos horários, que é sempre Brasília (America/Sao_Paulo).", example = "America/Sao_Paulo", deprecated = true)
        String timeZone
) {
    public EventDTO(Long id, String title, String slug, String summary, String content, String coverImageKey,
                    EnumEventCategory category, EnumEventFormat format,
                    EnumScheduleConflictPolicy scheduleConflictPolicy, EnumEventStatus status,
                    LocalDateTime startDate, LocalDateTime endDate, EnumEventExecutionStatus executionStatus,
                    LocalDateTime enrollmentStartDate, LocalDateTime enrollmentEndDate,
                    Boolean enrollmentPaused, EnumEnrollmentStatus enrollmentStatus, UUID ownerId,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String address,
                    BigDecimal workloadHours, BigDecimal teamCertificateWorkloadHours, String organization) {
        this(id, title, slug, summary, content, coverImageKey, category, format, scheduleConflictPolicy, status,
                startDate, endDate, executionStatus, enrollmentStartDate, enrollmentEndDate, enrollmentPaused,
                enrollmentStatus, ownerId, createdAt, updatedAt, address, workloadHours,
                teamCertificateWorkloadHours, organization, "America/Sao_Paulo");
    }
    public EventDTO(Long id, String title, String slug, String summary, String content, String coverImageKey,
                    EnumEventCategory category, EnumEventFormat format,
                    EnumScheduleConflictPolicy scheduleConflictPolicy, EnumEventStatus status,
                    LocalDateTime startDate, LocalDateTime endDate, EnumEventExecutionStatus executionStatus,
                    LocalDateTime enrollmentStartDate, LocalDateTime enrollmentEndDate,
                    Boolean enrollmentPaused, EnumEnrollmentStatus enrollmentStatus, UUID ownerId,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String address) {
        this(id, title, slug, summary, content, coverImageKey, category, format, scheduleConflictPolicy,
                status, startDate, endDate, executionStatus, enrollmentStartDate, enrollmentEndDate,
                enrollmentPaused, enrollmentStatus, ownerId, createdAt, updatedAt, address, null, null, null,
                "America/Sao_Paulo");
    }

    public boolean isPubliclyAccessible() {
        return status == EnumEventStatus.PUBLISHED; // ajuste para a regra real da sua entidade
    }

    public boolean isOwner(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isPublished() {
        return this.status == EnumEventStatus.PUBLISHED;
    }
}
