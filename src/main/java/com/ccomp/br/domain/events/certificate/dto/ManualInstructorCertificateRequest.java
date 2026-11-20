package com.ccomp.br.domain.events.certificate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

@Schema(description = "Informe exatamente um destinatário: instructorId para ministrante, ou eventGuestId junto com activityId para convidado. A combinação de instructorId e eventGuestId é inválida.")
public record ManualInstructorCertificateRequest(
        @Schema(description = "ID do vínculo confirmado de ministrante. Use sozinho para certificado geral; combine com activityId para certificado daquela atividade.", example = "4")
        @Positive Long instructorId,
        @Schema(description = "ID da atividade. Com instructorId, emite certificado específico e deve ser a atividade do vínculo. Com eventGuestId, é obrigatório e emite certificado do convidado para essa atividade.", example = "27")
        @Positive Long activityId,
        @Schema(description = "ID do perfil do convidado no evento (EventGuest), não o ID do vínculo ActivityGuest. Deve ser informado junto com activityId.", example = "20")
        @Positive Long eventGuestId
) {
    @AssertTrue(message = "Informe instructorId, opcionalmente com activityId, ou o par activityId e eventGuestId.")
    @Schema(hidden = true)
    public boolean isTargetValid() {
        return (instructorId != null && eventGuestId == null)
                || (instructorId == null && activityId != null && eventGuestId != null);
    }
}
