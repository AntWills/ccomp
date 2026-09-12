package com.ccomp.br.domain.events.editors.dto;

import java.time.LocalDateTime;

public record EventEditorCursor(LocalDateTime assignedAt, Long id) {
}
