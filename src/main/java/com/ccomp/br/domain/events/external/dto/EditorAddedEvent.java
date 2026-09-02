package com.ccomp.br.domain.events.external.dto;

import com.ccomp.br.shared.dto.UserDTO;

public record EditorAddedEvent(
        Long eventId,
        String eventTitle,
        String code,
        UserDTO editorUser,
        Long eventEditorId
) {
}
