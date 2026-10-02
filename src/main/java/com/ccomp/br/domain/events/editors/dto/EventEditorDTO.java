package com.ccomp.br.domain.events.editors.dto;

import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventEditorDTO (
        Long id,
        Long eventId,
        UUID userId,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt,
        EnumEditorsStatus status
) {
    public boolean isActive() {
        return status == EnumEditorsStatus.ACTIVE;
    }
}
