package com.ccomp.br.domain.events.dto.editors;

import java.time.LocalDateTime;

public record EventEditorCursor(LocalDateTime assignedAt, Long id) {
}
