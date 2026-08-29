package com.ccomp.br.domain.audit.dto;

import java.time.LocalDateTime;

public record AuditLogCursor(LocalDateTime timestamp, Long id) {
}
