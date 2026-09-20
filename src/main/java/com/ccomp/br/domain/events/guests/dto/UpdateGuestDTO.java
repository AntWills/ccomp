package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;

public record UpdateGuestDTO(
        EnumGuestStatus status,
        EnumGuestVisibility visibility
) {
}
