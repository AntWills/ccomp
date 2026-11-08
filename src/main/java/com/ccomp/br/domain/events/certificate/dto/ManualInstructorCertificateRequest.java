package com.ccomp.br.domain.events.certificate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

@Schema(description = "Identifica um ministrante já cadastrado para emissão manual pela equipe.")
public record ManualInstructorCertificateRequest(
        @Positive Long instructorId,
        @Positive Long activityId,
        @Positive Long activityGuestId
) {
    @AssertTrue(message = "Informe instructorId ou o par activityId e activityGuestId, sem misturar as opções.")
    @Schema(hidden = true)
    public boolean isTargetValid() {
        return (instructorId != null && activityId == null && activityGuestId == null)
                || (instructorId == null && activityId != null && activityGuestId != null);
    }
}
