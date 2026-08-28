package com.ccomp.br.domain.users.dto;

import com.ccomp.br.domain.users.enums.EnumActorType;
import com.ccomp.br.domain.users.persistence.audit.ChangeLog;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        String action,
        EnumActorType actorType,
        UUID actorId,
        UUID targetId,
        String reason,
        Map<String, ChangeLog> changes,
        LocalDateTime timestamp
) {}
