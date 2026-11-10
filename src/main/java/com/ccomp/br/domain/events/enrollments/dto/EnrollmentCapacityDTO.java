package com.ccomp.br.domain.events.enrollments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Total atual de inscrições ativas e limite de capacidade; maxCapacity nulo significa sem limite configurado.")
public record EnrollmentCapacityDTO(
        long currentEnrollments,
        Integer maxCapacity
) {}
