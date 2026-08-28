package com.ccomp.br.domain.users.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogCursor(LocalDateTime timestamp, Long id) {
}
