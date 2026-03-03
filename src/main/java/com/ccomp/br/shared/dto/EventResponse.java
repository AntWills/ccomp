package com.ccomp.br.shared.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        UUID ownerId
) {
}
