package com.ccomp.br.domain.audit.external.dto;

import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
import com.ccomp.br.domain.audit.external.enums.EnumTargetType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AuditCommand(
        UUID adminId,
        UUID targetId,
        String reason,
        String fieldName,
        String previousStatus,
        String newStatus,
        EnumTargetType targetType,
        EnumActorType actorType,
        EnumActionType actionType
) {
}
