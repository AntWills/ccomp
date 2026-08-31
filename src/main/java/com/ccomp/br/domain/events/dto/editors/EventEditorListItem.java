package com.ccomp.br.domain.events.dto.editors;

import com.ccomp.br.shared.dto.UserSummaryView;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventEditorListItem(
        Long id,
        Long eventId,
        UserSummaryView user,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt,
        boolean active
) {
}
