package com.ccomp.br.domain.events.editors.dto;

import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventEditorListItem(
        UserSummaryDTO user,
        Long id,
        Long eventId,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt,
        EnumEditorsStatus status
) {
}
