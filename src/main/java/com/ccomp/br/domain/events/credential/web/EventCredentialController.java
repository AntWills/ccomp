package com.ccomp.br.domain.events.credential.web;

import com.ccomp.br.domain.events.credential.application.EventCredentialService;
import com.ccomp.br.domain.events.credential.dto.*;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/credentials")
@Tag(name = "events/credentials", description = "Credenciamento de participantes, tokens QR e equipe de recepção de um evento.")
public class EventCredentialController {
    private final EventCredentialService service;

    public EventCredentialController(EventCredentialService service) {
        this.service = service;
    }

    @Operation(
            tags = {"DEV — NÃO INTEGRAR"},
            summary = "[DEV] Credencia participante por QR Code",
            description = """
                    **Status: DEV — não usar no frontend.** Rota mantida para uma possível futura recepção por QR Code;
                    o fluxo atual deve usar busca manual e credenciamento da inscrição.

                    Valida o token QR opaco da inscrição e registra a chegada ao evento junto com a credencial operacional.
                    Uma inscrição `CONFIRMED` passa para `CHECKED_IN`; uma inscrição já credenciada retorna resultado
                    idempotente. O QR precisa pertencer ao evento da rota e não pode ter sido rotacionado. Informe um
                    identificador curto da estação no corpo. Requer operador de credenciamento ou permissão de gestão do evento.

                    **Respostas:** `200` com resultado do credenciamento; `400` para corpo inválido; `401` sem JWT;
                    `403` sem permissão; `404` para evento/inscrição inexistente; `409` para QR ou inscrição não elegível.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado do credenciamento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token ou estação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para credenciar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou inscrição não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "QR revogado ou inscrição não elegível",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/scan")
    public ResponseEntity<CredentialResponse> scan(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Valid @RequestBody CredentialScanRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.scan(eventId, userId(jwt), request));
    }

    @Operation(
            summary = "Busca inscrições para credenciamento manual",
            description = """
                    Pesquisa inscrições do evento por nome ou e-mail. Retorna no máximo 20 resultados com nome,
                    e-mail mascarado, estado e indicadores de elegibilidade e credenciamento. CPF não é pesquisado
                    nem retornado por esta rota. Requer operador de credenciamento ou permissão de gestão do evento.

                    **Respostas:** `200` com os resultados; `400` se o termo for inválido; `401` sem JWT; `403` sem permissão.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscrições encontradas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialSearchItem.class))),
            @ApiResponse(responseCode = "400", description = "Termo inválido ou curto demais",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para consultar inscrições",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/manual/search")
    public ResponseEntity<List<CredentialSearchItem>> search(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Valid @RequestBody CredentialSearchRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.search(eventId, userId(jwt), request));
    }

    @Operation(
            summary = "Credencia uma inscrição manualmente",
            description = """
                    Registra a chegada após a equipe localizar e confirmar a inscrição. O endpoint aplica as mesmas
                    regras de estado do scan: somente `CONFIRMED` pode ser credenciada; `CHECKED_IN` existente é
                    tratado de forma idempotente ou reconciliado. Inscrições `CANCELED` e `BANNED` são recusadas.
                    Requer operador de credenciamento ou permissão de gestão do evento.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado do credenciamento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corpo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para credenciar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou inscrição não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inscrição cancelada ou bloqueada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/manual")
    public ResponseEntity<CredentialResponse> manual(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Valid @RequestBody ManualCredentialRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.manual(eventId, userId(jwt), request));
    }

    @Operation(
            tags = {"DEV — NÃO INTEGRAR"},
            summary = "[DEV] Registra impressão ou reimpressão de crachá",
            description = """
                    **Status: DEV — não usar no frontend.** O fluxo atual não controla impressão nem gera arquivo de crachá.
                    A rota permanece documentada para uma futura etapa de impressão.

                    Incrementa o contador de impressões de uma credencial já registrada e retorna os dados mínimos
                    do participante para a estação produzir o crachá. A rota não gera um PDF nem altera a presença.
                    Informe a estação que realizou a impressão. Cada chamada conta como uma impressão; evite repetir
                    automaticamente uma chamada após timeout. Requer operador de credenciamento ou gestão do evento.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Impressão registrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estação inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para imprimir",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credencial não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Participante ainda não credenciado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{credentialId}/reprint")
    public ResponseEntity<CredentialResponse> reprint(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Parameter(description = "ID da credencial", example = "15") @PathVariable Long credentialId,
            @Valid @RequestBody BadgePrintRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.reprint(eventId, credentialId, userId(jwt), request.station()));
    }

    @Operation(
            tags = {"DEV — NÃO INTEGRAR"},
            summary = "[DEV] Emite o QR pessoal da inscrição",
            description = """
                    **Status: DEV — não usar no frontend.** Só será necessária se o fluxo de recepção passar a usar QR Code.

                    Cria um token aleatório opaco para o participante autenticado e o evento informado. O token bruto
                    é devolvido somente na emissão inicial; o banco persiste apenas seu hash. Se já houver token,
                    esta rota não o recupera: use a rota de rotação para invalidar o antigo e receber outro.
                    A inscrição deve estar elegível e pertencer ao usuário autenticado.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token pessoal emitido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Inscrição não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inscrição inelegível ou QR já emitido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/my-token")
    public ResponseEntity<CredentialTokenResponse> myToken(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.myToken(eventId, userId(jwt)));
    }

    @Operation(
            tags = {"DEV — NÃO INTEGRAR"},
            summary = "[DEV] Rotaciona o QR pessoal",
            description = "**Status: DEV — não usar no frontend.** Só será necessária se o fluxo de recepção passar a usar QR Code. Revoga o token anterior e emite outro para a inscrição do usuário autenticado; não altera a presença no evento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Novo token emitido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Inscrição não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inscrição inelegível",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/my-token/rotate")
    public ResponseEntity<CredentialTokenResponse> rotateMyToken(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.rotateMyToken(eventId, userId(jwt)));
    }

    @Operation(
            tags = {"DEV — NÃO INTEGRAR"},
            summary = "[DEV] Rotaciona o QR de uma inscrição",
            description = "**Status: DEV — não usar no frontend.** Só será necessária se o fluxo de recepção passar a usar QR Code. Invalida o token atual e emite outro. Esta ação não desfaz o credenciamento nem modifica a inscrição; exige operador ou gestão do evento e fica auditada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Novo token emitido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para rotacionar QR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Inscrição não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inscrição inelegível",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/enrollments/{enrollmentId}/token/rotate")
    public ResponseEntity<CredentialTokenResponse> rotateParticipantToken(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Parameter(description = "ID da inscrição", example = "203") @PathVariable Long enrollmentId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.rotateParticipantToken(eventId, enrollmentId, userId(jwt)));
    }

    @Operation(
            summary = "Consulta o resumo do credenciamento",
            description = "Retorna totais do evento: inscrições elegíveis, participantes credenciados e restantes. A resposta é agregada e não expõe dados pessoais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CredentialSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para consultar o resumo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content)
    })
    @GetMapping("/summary")
    public ResponseEntity<CredentialSummaryResponse> summary(@Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.summary(eventId, userId(jwt)));
    }

    @Operation(
            summary = "Concede acesso de operador de credenciamento",
            description = "Autoriza um usuário com conta ativa a operar o credenciamento deste evento sem conceder permissão geral de edição. Somente proprietário, editor ativo, MODERATOR ou ADMIN podem conceder acesso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Acesso concedido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerir operadores",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conta do usuário não está ativa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/operators/{userId}")
    public ResponseEntity<Void> addOperator(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Parameter(description = "UUID do usuário que receberá acesso operacional") @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        service.addOperator(eventId, userId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Remove acesso de operador de credenciamento",
            description = "Revoga o acesso operacional concedido para este evento. Não remove credenciamentos já registrados. Somente usuários com permissão de gestão podem revogar operadores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Acesso removido ou já inexistente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerir operadores",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content)
    })
    @DeleteMapping("/operators/{operatorUserId}")
    public ResponseEntity<Void> removeOperator(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Parameter(description = "UUID do operador a remover") @PathVariable UUID operatorUserId,
            @AuthenticationPrincipal Jwt jwt) {
        service.removeOperator(eventId, operatorUserId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
