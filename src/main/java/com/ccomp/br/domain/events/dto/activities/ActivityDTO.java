package com.ccomp.br.domain.events.dto.activities;

public record ActivityDTO(
        Long id,
        Long eventId,
        String title,
        String description
) {
}
