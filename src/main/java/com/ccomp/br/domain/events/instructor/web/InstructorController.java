package com.ccomp.br.domain.events.instructor.web;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.instructor.application.InstructorInvitationService;
import com.ccomp.br.domain.events.instructor.application.InstructorService;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitation;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(
        name = "events/instructors (ministrantes)",
        description = "Consulta e gestão de ministrantes vinculados às atividades: atualização da atividade pelo ministrante, convites por e-mail, respostas e cancelamento do próprio vínculo."
)
public class InstructorController {
    private final InstructorService instructorService;
    private final InstructorInvitationService invitationService;

    public InstructorController(
            InstructorService instructorService,
            InstructorInvitationService invitationService
    ) {
        this.instructorService = instructorService;
        this.invitationService = invitationService;
    }

    @Operation(
            summary = "Lista ministrantes de um evento",
            description = "Retorna uma página cursorada com os vínculos de ministrantes confirmados nas atividades do evento. Requer autenticação; a autorização para visualizar depende das regras de visibilidade do evento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de ministrantes retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar os ministrantes"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/{eventId}/instructors")
    public ResponseEntity<CursorPage<EventInstructor>> searchByEvent(
            @Parameter(description = "ID do evento", example = "1")
            @PathVariable Long eventId,
            @Parameter(description = "Cursor retornado pela página anterior")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Itens por página (máximo 50)", example = "20")
            @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                instructorService.searchByEvent(eventId, extractUserId(jwt), cursor, pageSize)
        );
    }

    @Operation(
            summary = "Atualiza uma atividade como ministrante",
            description = "Permite ao usuário autenticado atualizar os dados da atividade quando possui vínculo confirmado como ministrante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atividade atualizada"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é ministrante confirmado desta atividade"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos para atualização")
    })
    @PatchMapping("/activities/{activityId}/instructors/me")
    public ResponseEntity<ActivityDTO> updateActivityAsInstructor(
            @Parameter(description = "ID da atividade", example = "1")
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                instructorService.updateActivity(extractUserId(jwt), activityId, request)
        );
    }

    @Operation(
            summary = "Cancela o próprio vínculo de ministrante",
            description = "Altera para cancelado o vínculo confirmado do usuário autenticado com a atividade. O vínculo continua registrado e pode ser reativado por um novo convite aceito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vínculo cancelado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "400", description = "Vínculo já está cancelado"),
            @ApiResponse(responseCode = "403", description = "Usuário não possui vínculo com a atividade")
    })
    @DeleteMapping("/activities/{activityId}/instructors/me")
    public ResponseEntity<MessageResponse> cancelMyInstructorLink(
            @Parameter(description = "ID da atividade", example = "1")
            @PathVariable Long activityId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(instructorService.cancelMyLink(extractUserId(jwt), activityId));
    }

    @Operation(
            summary = "Convida uma pessoa para ministrar atividade",
            description = "Envia um convite ao e-mail informado. Restrito a quem pode editar o evento associado à atividade. O convite expira em 24 horas; um convite anterior ainda enviado para o mesmo e-mail e atividade é cancelado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite criado e envio solicitado"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido ou regra de negócio violada"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar ministrantes do evento"),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado")
    })
    @PostMapping("/activities/{activityId}/instructors/invitations")
    public ResponseEntity<MessageResponse> invite(
            @Parameter(description = "ID da atividade", example = "1")
            @PathVariable Long activityId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "E-mail da pessoa convidada",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @Schema(implementation = InviteInstructorRequest.class)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody InviteInstructorRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(invitationService.invite(
                extractUserId(jwt),
                activityId,
                new EmailAddress(request.email())
        ));
    }

    @Operation(
            summary = "Lista convites de ministrante de uma atividade",
            description = "Retorna os convites da atividade, incluindo seus status e prazos. Restrito a quem pode editar o evento associado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convites retornados"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar ministrantes do evento"),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado")
    })
    @GetMapping("/activities/{activityId}/instructors/invitations")
    public ResponseEntity<List<InstructorInvitation>> searchInvitations(
            @Parameter(description = "ID da atividade", example = "1")
            @PathVariable Long activityId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(invitationService.searchInvitations(extractUserId(jwt), activityId));
    }

    @Operation(
            summary = "Aceita ou recusa convite de ministrante",
            description = "Responde ao convite identificado pelo código recebido por e-mail. A conta autenticada deve corresponder ao e-mail convidado. Por padrão, o convite é aceito; use `accept=false` para recusá-lo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite aceito ou recusado"),
            @ApiResponse(responseCode = "400", description = "Convite expirado, inválido ou já respondido"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Convite destinado a outra conta"),
            @ApiResponse(responseCode = "404", description = "Código de convite não encontrado")
    })
    @PostMapping("/instructor-invitations/{code}/reply")
    public ResponseEntity<MessageResponse> reply(
            @Parameter(description = "Código UUID recebido no convite", example = "a7bc9ded-36bf-45db-8125-6bbdbead98c8")
            @PathVariable UUID code,
            @Parameter(description = "Aceitar (`true`) ou recusar (`false`); padrão: `true`", example = "true")
            @RequestParam(defaultValue = "true") boolean accept,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(invitationService.reply(code, extractUserId(jwt), accept));
    }

    @Operation(
            summary = "Cancela um convite de ministrante",
            description = "Cancela um convite ainda enviado. Convites aceitos ou recusados já foram respondidos e não podem ser cancelados. Restrito a quem pode editar o evento associado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite cancelado"),
            @ApiResponse(responseCode = "400", description = "Convite já respondido ou cancelado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar ministrantes do evento"),
            @ApiResponse(responseCode = "404", description = "Convite ou evento não encontrado")
    })
    @DeleteMapping("/instructor-invitations/{invitationId}")
    public ResponseEntity<MessageResponse> cancel(
            @Parameter(description = "ID do convite", example = "1")
            @PathVariable Long invitationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(invitationService.cancel(extractUserId(jwt), invitationId));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @Schema(description = "Dados necessários para convidar um ministrante")
    public record InviteInstructorRequest(
            @Schema(description = "E-mail da pessoa convidada", example = "ministrante@exemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Email String email
    ) {
    }
}
