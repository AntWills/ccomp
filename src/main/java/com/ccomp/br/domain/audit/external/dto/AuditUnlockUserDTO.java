package com.ccomp.br.domain.audit.external.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuditUnlockUserDTO(
        UUID adminId,
        UUID targetId,
        String reason,
        String previousStatus,
        String newStatus
) {
}
