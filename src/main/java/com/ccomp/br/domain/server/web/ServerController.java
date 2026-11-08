package com.ccomp.br.domain.server.web;

import com.ccomp.br.domain.server.application.ServerStatusService;
import com.ccomp.br.domain.server.dto.ServerStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "server", description = "Informações de horário e recursos do servidor")
@RestController
@RequestMapping("/api/server")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@RequiredArgsConstructor
public class ServerController {
    private final ServerStatusService serverStatusService;

    @Operation(
            summary = "Consultar status do servidor",
            description = "Retorna o horário atual do servidor e o uso de memória heap e CPU. Requer perfil ADMIN ou MODERATOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status do servidor retornado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServerStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado; requer perfil ADMIN ou MODERATOR", content = @Content)
    })
    @GetMapping("/status")
    public ServerStatusResponse getStatus() {
        return serverStatusService.getStatus();
    }
}
