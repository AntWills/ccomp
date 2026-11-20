package com.ccomp.br.domain.events.certificate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

@Schema(description = "Identifica um ministrante ou convidado para emissão manual pela equipe. Para convidado, informe também o ID da atividade.")
public record ManualInstructorCertificateRequest(
        @Positive Long instructorId,
        @Positive Long activityId,
        @Positive Long eventGuestId
) {
    @AssertTrue(message = "Informe instructorId, opcionalmente com activityId, ou o par activityId e eventGuestId.")
    @Schema(hidden = true)
    public boolean isTargetValid() {
        return (instructorId != null && eventGuestId == null)
                || (instructorId == null && activityId != null && eventGuestId != null);
    }
}
