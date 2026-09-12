package com.ccomp.br.domain.events.editors.dto;

import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
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
        EnumEditorsStatus status
) {
}
