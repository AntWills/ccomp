package com.ccomp.br.domain.events.external.dto;

import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;

import java.util.UUID;

public record EditorAddedEvent(
        Long eventId,
        String eventTitle,
        UUID code,
        EmailAddress emailAddress
) {
}
