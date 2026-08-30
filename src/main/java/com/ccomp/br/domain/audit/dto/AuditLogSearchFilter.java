package com.ccomp.br.domain.audit.dto;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public record AuditLogSearchFilter(
        UUID actorId,
        UUID targetId,
        String action,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public Optional<UUID> optActorId() {
        return Optional.ofNullable(actorId);
    }

    public Optional<UUID> optTargetId() {
        return Optional.ofNullable(targetId);
    }

    public Optional<String> optAction() {
        return Optional.ofNullable(action)
                .filter(s -> !s.isBlank());
    }

    public Optional<LocalDateTime> optStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDateTime> optEndDate() {
        return Optional.ofNullable(endDate);
    }
}
