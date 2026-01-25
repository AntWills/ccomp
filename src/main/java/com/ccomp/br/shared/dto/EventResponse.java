package com.ccomp.br.shared.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EventResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        UUID ownerId
) {
}
