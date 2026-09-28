package com.ccomp.br.domain.events.activities.web;

import com.ccomp.br.domain.events.activities.application.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

        BufferedImage image = checkInService.generateCode(activityId, extractUserId(jwt));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(baos.toByteArray());
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
