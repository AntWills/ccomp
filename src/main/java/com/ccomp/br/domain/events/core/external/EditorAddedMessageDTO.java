package com.ccomp.br.domain.events.core.external;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public record EditorAddedMessageDTO(
        Long eventId,
        String eventTitle,
        UUID code,
        EmailAddress emailAddress
) {
}
