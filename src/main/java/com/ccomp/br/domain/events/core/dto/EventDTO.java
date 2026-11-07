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
        LocalDateTime updatedAt,

        // Endereço / local do evento (usado nos certificados em {evento.local})
        String address,
        @Schema(description = "Carga horária base do evento, somada às atividades com presença no certificado do participante.")
        BigDecimal workloadHours,
        @Schema(description = "Carga horária configurada para certificados do owner e de editores ativos.")
        BigDecimal teamCertificateWorkloadHours,
        @Schema(description = "Organização responsável pelo evento.")
        String organization
) {
    public EventDTO(Long id, String title, String slug, String summary, String content, String coverImageKey,
                    EnumEventCategory category, EnumEventFormat format,
                    EnumScheduleConflictPolicy scheduleConflictPolicy, EnumEventStatus status,
                    LocalDateTime startDate, LocalDateTime endDate, EnumEventExecutionStatus executionStatus,
                    LocalDateTime enrollmentStartDate, LocalDateTime enrollmentEndDate,
                    Boolean enrollmentPaused, EnumEnrollmentStatus enrollmentStatus, UUID ownerId,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String address) {
        this(id, title, slug, summary, content, coverImageKey, category, format, scheduleConflictPolicy,
                status, startDate, endDate, executionStatus, enrollmentStartDate, enrollmentEndDate,
                enrollmentPaused, enrollmentStatus, ownerId, createdAt, updatedAt, address, null, null, null);
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
