package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.activities.application.CheckInService;
import com.ccomp.br.domain.events.activities.dto.CheckInRequest;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "events/activity/check-in (CheckIn)")
@RestController
@RequestMapping("api/events")
public class CheckInController {
    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping(value = "/activity/{activityId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(
            summary = "Gera o QR Code de presença",
            description = "Retorna o QR Code que a equipe autorizada deve exibir para registrar a presença dos participantes inscritos nesta atividade. O código pode ser gerado antes do início; sua leitura só é aceita durante o período da atividade. Gerar o código não registra presença nem credenciamento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "QR Code gerado com sucesso",
                    content = @Content(
                            mediaType = MediaType.IMAGE_PNG_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão", content = @Content),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Falha ao gerar a imagem do QR Code", content = @Content)
    })
    public ResponseEntity<byte[]> getQRCode(
            @PathVariable long activityId,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        var qr = checkInService.generateCode(activityId, extractUserId(jwt));

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qr);
    }

    @PostMapping("/activity/{activityId}/check-in")
    @Operation(
            summary = "Confirma a presença na atividade",
            description = "Registra a participação do usuário autenticado nesta atividade após validar o QR Code. "
                    + "A inscrição no evento deve estar ativa (`CONFIRMED` ou `CHECKED_IN`) e vinculada à atividade. "
                    + "A atividade precisa ter início e término configurados, e o horário deve estar estritamente entre esses limites. "
                    + "A presença na atividade é salva uma única vez; em seguida, o sistema atualiza o check-in do evento "
                    + "e envia por e-mail um aviso de que o certificado da atividade está disponível. A atualização do check-in do evento e o envio do e-mail são assíncronos. "
                    + "Credenciamento operacional não é alterado por esta rota."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presença confirmada com sucesso", content = @Content),
            @ApiResponse(
                    responseCode = "400",
                    description = "Código inválido, atividade sem período configurado ou fora do horário, ou inscrição inativa",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Atividade ou inscrição ativa na atividade não encontrada",
                    content = @Content
            ),
            @ApiResponse(responseCode = "500", description = "Falha ao persistir a presença ou publicar a atualização assíncrona", content = @Content)
    })
    public ResponseEntity<MessageResponse> checkIn(
            @PathVariable long activityId,
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        checkInService.checkIn(activityId, extractUserId(jwt), request.code());

        return ResponseEntity.ok(
                new MessageResponse("Check-in realizado com sucesso.")
        );
    }

    @PostMapping("/activity/{activityId}/enrollments/{enrollmentId}/attendance")
    @Operation(summary = "Marca manualmente a presença de uma pessoa na atividade",
            description = "Operação de gerência. A pessoa precisa estar inscrita na atividade e ter inscrição ativa no evento. Registra a presença e dispara o fluxo de certificado da atividade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presença registrada"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "404", description = "Atividade ou inscrição na atividade não encontrada"),
            @ApiResponse(responseCode = "409", description = "Inscrição no evento inativa")
    })
    public ResponseEntity<MessageResponse> markAttendanceManually(
            @PathVariable long activityId,
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        checkInService.markAttendanceManually(activityId, enrollmentId, extractUserId(jwt));
        return ResponseEntity.ok(new MessageResponse("Presença registrada com sucesso."));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
