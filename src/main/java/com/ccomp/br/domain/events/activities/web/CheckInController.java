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

@Tag(name = "events/check-in (CheckIn)")
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
            description = "Retorna a imagem PNG do QR Code para validação de presença na atividade."
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
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada", content = @Content)
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
            description = "Registra a presença do usuário autenticado na atividade, "
                    + "validando o código lido no QR Code. O usuário precisa estar inscrito "
                    + "na atividade e ela precisa estar em andamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Presença confirmada com sucesso", content = @Content),
            @ApiResponse(
                    responseCode = "400",
                    description = "Código inválido ou expirado, atividade fora do horário de check-in, ou corpo da requisição inválido",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Atividade não encontrada ou usuário não inscrito na atividade",
                    content = @Content
            )
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

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
