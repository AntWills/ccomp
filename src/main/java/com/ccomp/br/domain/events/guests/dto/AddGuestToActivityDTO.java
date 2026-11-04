package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;

public record AddGuestToActivityDTO(EnumActivityGuestRole role) {
}
