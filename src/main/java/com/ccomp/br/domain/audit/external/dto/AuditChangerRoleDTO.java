package com.ccomp.br.domain.audit.external.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuditChangerRoleDTO(
        UUID adminId,
        UUID targetId,
        String previousStatus,
        String newStatus
) {
}
