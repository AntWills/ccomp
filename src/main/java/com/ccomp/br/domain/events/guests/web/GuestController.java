package com.ccomp.br.domain.events.guests.web;

import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.guests.application.GuestImageService;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.dto.AddGuestToActivityDTO;
import com.ccomp.br.domain.events.guests.dto.CreateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.LinkGuestAccountDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(
        name = "events/guests (convidados)",
        description = "Cadastro e consulta de convidados, associação a atividades, edição de perfil e imagens."
)
public class GuestController {

    private final GuestService guestService;
    private final GuestImageService guestImageService;

    public GuestController(GuestService guestService, GuestImageService guestImageService) {
        this.guestService = guestService;
        this.guestImageService = guestImageService;
    }

    @PostMapping("/{eventId}/guests")
    @Operation(
            summary = "Adiciona um convidado ao evento",
            description = "Cria o perfil público de um convidado sem exigir conta ou e-mail. O corpo aceita `name` " +
                    "e `description`. O convidado fica associado ao evento; atividades são vinculadas por uma rota própria. Requer JWT " +
                    "de proprietário, editor ativo, MODERATOR ou ADMIN. Retorna `201` com `MessageResponse`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convidado cadastrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nome ausente, campo acima do limite ou corpo inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para editar o evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> addGuest(
            @Parameter(description = "ID do evento", example = "12") @PathVariable Long eventId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGuestDTO request
    ) {
        guestService.createGuest(eventId, extractUserId(jwt), request);

        return ResponseEntity.status(201)
                .body(new MessageResponse("Convidado adicionado com sucesso ao evento."));
    }

    @PostMapping("/{eventId}/guests/{guestId}/activities/{activityId}")
    @Operation(
            summary = "Associa um convidado existente a uma atividade",
            description = "Vincula um convidado já cadastrado no evento à atividade indicada, sem recriar seu perfil. " +
                    "O corpo JSON é opcional e aceita `role`: `PRESENTER` (ministrante), `SPECIAL_GUEST` " +
                    "(convidado especial), `MODERATOR` (mediador) ou `LISTENER` (ouvinte). Sem corpo ou sem `role`, " +
                    "o padrão é `PRESENTER`. Se o vínculo já existir e a função enviada for diferente, atualiza a função. " +
                    "A atividade precisa pertencer ao mesmo evento. Requer JWT de proprietário, editor ativo, MODERATOR " +
                    "ou ADMIN. Retorna `204` sem corpo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convidado associado à atividade", content = @Content),
            @ApiResponse(responseCode = "400", description = "Valor de `role` inválido ou corpo JSON malformado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para editar o evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, convidado ou atividade não encontrado, ou atividade fora do evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> addGuestToActivity(
            @Parameter(description = "ID do evento", example = "12") @PathVariable Long eventId,
            @Parameter(description = "ID do convidado já cadastrado no evento", example = "35")
            @PathVariable Long guestId,
            @Parameter(description = "ID da atividade que receberá o convidado", example = "78")
            @PathVariable Long activityId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) AddGuestToActivityDTO request
    ) {
        guestService.addGuestToActivity(
                eventId,
                guestId,
                activityId,
                request == null ? null : request.role(),
                extractUserId(jwt)
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{eventId}/guests/{guestId}/account")
    @Operation(
            summary = "Vincula uma conta existente do convidado a uma atividade",
            description = "O corpo aceita `email` e `activity_id`. Localiza uma conta ativa pelo e-mail informado, associa essa conta ao perfil do convidado " +
                    "e dá a ela permissão para editar a atividade selecionada. O e-mail não é gravado no perfil público. " +
                    "Requer JWT de proprietário, editor ativo, MODERATOR ou ADMIN. Retorna `204` sem corpo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta vinculada à atividade", content = @Content),
            @ApiResponse(responseCode = "400", description = "E-mail inválido, atividade ausente ou corpo malformado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para editar o evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convidado, evento, atividade ou conta ativa não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> linkGuestAccount(
            @Parameter(description = "ID do evento", example = "12") @PathVariable Long eventId,
            @Parameter(description = "ID do convidado", example = "35") @PathVariable Long guestId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LinkGuestAccountDTO request
    ) {
        guestService.linkAccount(
                eventId,
                guestId,
                extractUserId(jwt),
                request.email(),
                request.activityId()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/guests")
    @Operation(
            summary = "Lista os convidados de um evento",
            description = "Retorna `CursorPage<EventGuestDTO>` com `content`, `next_cursor` e `previous_cursor`. " +
                    "Cada item contém `id`, `event_id`, `name`, `description`, `image_key`, `status`, `visibility` e `created_at`; " +
                    "A lista pública contém apenas convidados confirmados e públicos; proprietário, editor ativo, " +
                    "MODERATOR ou ADMIN também podem consultar perfis privados e cancelados. `pageSize` usa padrão 20 " +
                    "e máximo 50; `cursor` é opaco e deve ser reutilizado com o mesmo evento. Acesso anônimo é permitido " +
                    "para eventos PUBLISHED; outros status exigem proprietário, editor ativo, MODERATOR ou ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de convidados retornada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CursorPage.class))),
            @ApiResponse(responseCode = "400", description = "ID, cursor ou pageSize inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Evento não pode ser visualizado pelo solicitante",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CursorPage<EventGuestDTO>> searchEventGuests(
            @Parameter(hidden = true) @AuthenticationPrincipal @Nullable Jwt jwt,
            @Parameter(description = "ID do evento", example = "12") @PathVariable Long eventId,
            @Parameter(description = "Cursor opaco da página anterior") @RequestParam(required = false) String cursor,
            @Parameter(description = "Quantidade de itens; padrão 20, máximo 50", example = "20")
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(
                guestService.searchEventGuests(eventId, extractUserId(jwt), cursor, pageSize)
        );
    }

    @GetMapping("/activities/{activityId}/guests")
    @Operation(
            summary = "Lista os convidados associados a uma atividade",
            description = "Retorna `CursorPage<ActivityGuestDTO>` com `content`, `next_cursor` e `previous_cursor`. " +
                    "Cada item contém `id`, `activity_id`, `event_guest_id`, `name`, `description`, `image_key`, " +
                    "`role` (`PRESENTER`, `SPECIAL_GUEST`, `MODERATOR` ou `LISTENER`), `status`, `visibility` e `created_at`; " +
                    "A lista pública contém apenas convidados confirmados e públicos; proprietário, editor ativo, " +
                    "MODERATOR ou ADMIN também podem consultar perfis privados e cancelados. `pageSize` usa padrão 20 " +
                    "e máximo 50; `cursor` é opaco e deve ser reutilizado com a mesma atividade. O acesso segue a " +
                    "visibilidade do evento pai."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de convidados da atividade retornada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CursorPage.class))),
            @ApiResponse(responseCode = "400", description = "ID, cursor ou pageSize inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Evento pai não pode ser visualizado pelo solicitante",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento pai não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CursorPage<ActivityGuestDTO>> searchActivityGuests(
            @AuthenticationPrincipal @Nullable Jwt jwt,
            @Parameter(description = "ID da atividade", example = "78") @PathVariable Long activityId,
            @Parameter(description = "Cursor opaco da página anterior") @RequestParam(required = false) String cursor,
            @Parameter(description = "Quantidade de itens; padrão 20, máximo 50", example = "20")
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(
                guestService.searchActivityGuests(activityId, extractUserId(jwt), cursor, pageSize)
        );
    }

    @PatchMapping("/{eventId}/guests/{guestId}")
    @Operation(
            summary = "Atualizar status e visibilidade do convidado",
            description = "O corpo JSON aceita `status` (`CONFIRMED` ou `CANCELED`) e `visibility` (`PUBLIC` ou " +
                    "`PRIVATE`). Atualiza parcialmente esses campos; nulos ou omitidos ficam inalterados. " +
                    "Permitido ao convidado com conta vinculada " +
                    "ou a proprietário, editor ativo, MODERATOR ou ADMIN. Retorna `MessageResponse` com confirmação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferências atualizadas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Status, visibilidade ou ID inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante não é o convidado vinculado nem gerente do evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convidado ou evento não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> updateGuest(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody UpdateGuestDTO request
    ) {
        guestService.updateEventGuest(eventId, guestId, extractUserId(jwt), request);
        return ResponseEntity.ok(new MessageResponse("Preferências do convidado atualizadas com sucesso."));
    }

    @PatchMapping("/{eventId}/guests/{guestId}/profile")
    @Operation(
            summary = "Atualiza os dados do perfil do convidado",
            description = "O corpo aceita `name` e `description`. Substitui esses campos do perfil; `name` é obrigatório e não pode ficar em branco. " +
                    "Imagem é gerenciada pelas rotas `/image`. Requer JWT de proprietário, editor ativo, MODERATOR ou " +
                    "ADMIN. Retorna `MessageResponse` com confirmação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do convidado atualizados"),
            @ApiResponse(responseCode = "400", description = "Nome ausente, campo acima do limite ou corpo inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o convidado"),
            @ApiResponse(responseCode = "404", description = "Evento ou convidado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> updateGuestProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody UpdateGuestProfileDTO request
    ) {
        guestService.updateGuestProfile(eventId, guestId, extractUserId(jwt), request);
        return ResponseEntity.ok(new MessageResponse("Dados do convidado atualizados com sucesso."));
    }

    @PostMapping(value = "/{eventId}/guests/{guestId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Envia ou substitui a imagem do convidado",
            description = "Recebe o campo multipart `file`; aceita PNG, JPEG e WebP. Os bytes são enviados ao object " +
                    "storage e a chave resultante é associada ao perfil. A imagem anterior é removida após a substituição. " +
                    "Requer JWT de proprietário, editor ativo, MODERATOR ou ADMIN. Retorna `MessageResponse`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem salva",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente/vazio ou tipo diferente de PNG, JPEG ou WebP",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para editar o evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou convidado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada ao salvar a imagem no storage",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> uploadGuestImage(
            @Parameter(description = "ID do evento", example = "12") @PathVariable Long eventId,
            @Parameter(description = "ID do convidado", example = "35") @PathVariable Long guestId,
            @Parameter(description = "Arquivo PNG, JPEG ou WebP", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        guestImageService.uploadImage(eventId, guestId, extractUserId(jwt), file);
        return ResponseEntity.ok(new MessageResponse("Imagem do convidado atualizada com sucesso."));
    }

    @GetMapping("/{eventId}/guests/{guestId}/image")
    @Operation(
            summary = "Retorna a imagem do convidado",
            description = "Transmite os bytes da imagem com o Content-Type armazenado. Evento PUBLISHED ou UNLISTED " +
                    "pode ser consultado anonimamente; demais estados exigem proprietário, editor ativo, MODERATOR ou " +
                    "ADMIN. Retorna `204` se o convidado não tiver imagem ou o objeto não existir no storage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada",
                    content = {
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary"))
                    }),
            @ApiResponse(responseCode = "204", description = "Imagem ausente", content = @Content),
            @ApiResponse(responseCode = "403", description = "Evento privado e usuário sem permissão de visualização",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou convidado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada ao ler a imagem do storage",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> getGuestImage(
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        return guestImageService.getImage(eventId, guestId, extractUserId(jwt))
                .map(file -> {
                    MediaType mediaType = file.contentType() == null
                            ? MediaTypeFactory.getMediaType("file").orElse(MediaType.APPLICATION_OCTET_STREAM)
                            : MediaType.parseMediaType(file.contentType());

                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(file.resource());
                })
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{eventId}/guests/{guestId}/image")
    @Operation(
            summary = "Remove a imagem do convidado",
            description = "Apaga o objeto do storage e limpa a chave associada ao convidado. Requer JWT de proprietário, " +
                    "editor ativo, MODERATOR ou ADMIN. Retorna `204` quando removida; retorna `404` se o perfil não " +
                    "tiver imagem associada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagem removida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para editar o evento",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, convidado ou imagem não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada ao remover imagem do storage",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> removeGuestImage(
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        boolean removed = guestImageService.removeImage(eventId, guestId, extractUserId(jwt));
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private UUID extractUserId(@Nullable Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
