package com.ccomp.br.domain.users.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogSearchFilter(
        UUID actorId,
        UUID targetId,
        String action,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
