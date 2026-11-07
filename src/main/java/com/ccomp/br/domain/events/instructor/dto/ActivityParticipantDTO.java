package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Participante vinculado a uma atividade, ministrante ou convidado")
public record ActivityParticipantDTO(
        @Schema(description = "ID do registro de ministrante ou do vínculo de convidado", example = "15")
        Long id,
        @Schema(description = "ID da conta vinculada ao ministrante; null para convidados ou perfis sem conta")
        UUID userId,
        @Schema(description = "ID da atividade", example = "7")
        Long activityId,
        @Schema(description = "Status do vínculo", example = "CONFIRMED")
        EnumInstructorStatus status,
        @Schema(description = "Nome público", example = "Ana Silva")
        String name,
        @Schema(description = "Descrição pública")
        String description,
        @Schema(description = "Função na atividade. Para ministrantes: INSTRUCTOR ou ASSISTANT; para convidados: papel de convidado.", example = "ASSISTANT")
        String role,
        @Schema(description = "Indica se o registro representa um convidado", example = "false")
        boolean isGuest,
        @Schema(description = "Data de criação do vínculo")
        LocalDateTime createdAt
) {
    public ActivityParticipantDTO(
            Long id,
            UUID userId,
            Long activityId,
            String status,
            String name,
            String description,
            String role,
            boolean isGuest,
            LocalDateTime createdAt
    ) {
        this(id, userId, activityId, EnumInstructorStatus.valueOf(status), name, description, role, isGuest, createdAt);
    }
}
