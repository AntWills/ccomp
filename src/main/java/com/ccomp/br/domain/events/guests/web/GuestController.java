package com.ccomp.br.domain.events.guests.web;

import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.guests.application.GuestImageService;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.dto.CreateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.LinkGuestAccountDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "events/guests (convidados)", description = "Gestão da lista de convidados de eventos e atividades")
public class GuestController {

    private final GuestService guestService;
    private final GuestImageService guestImageService;

    public GuestController(GuestService guestService, GuestImageService guestImageService) {
        this.guestService = guestService;
        this.guestImageService = guestImageService;
    }

    @PostMapping("/{eventId}/guests")
    @Operation(summary = "Adicionar convidado ao evento")
    public ResponseEntity<Long> addGuest(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @Valid @RequestBody CreateGuestDTO request
    ) {
        return ResponseEntity.status(201).body(guestService.createGuest(eventId, userId, request).getId());
    }

    @PatchMapping("/{eventId}/guests/{guestId}/account")
    @Operation(summary = "Vincular conta do convidado à atividade")
    public ResponseEntity<Void> linkGuestAccount(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody LinkGuestAccountDTO request
    ) {
        guestService.linkAccount(eventId, guestId, userId, request.email(), request.activityId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/guests")
    @Operation(summary = "Buscar convidados do evento", description = "Retorna de forma paginada (cursor) os convidados do evento.")
    public ResponseEntity<CursorPage<EventGuestDTO>> searchEventGuests(
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable Long eventId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(guestService.searchEventGuests(eventId, userId, cursor, pageSize));
    }

    @GetMapping("/activities/{activityId}/guests")
    @Operation(summary = "Buscar convidados da atividade", description = "Retorna de forma paginada (cursor) os convidados vinculados à atividade.")
    public ResponseEntity<CursorPage<ActivityGuestDTO>> searchActivityGuests(
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable Long activityId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(guestService.searchActivityGuests(activityId, userId, cursor, pageSize));
    }

    @PatchMapping("/{eventId}/guests/{guestId}")
    @Operation(
            summary = "Atualizar status e visibilidade do convidado",
            description = "Permite ao próprio convidado ou gerente do evento alterar presença e privacidade."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convidado atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para alterar este convidado")
    })
    public ResponseEntity<MessageResponse> updateGuest(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody UpdateGuestDTO request
    ) {
        guestService.updateEventGuest(eventId, guestId, userId, request);
        return ResponseEntity.ok(new MessageResponse("Preferências do convidado atualizadas com sucesso."));
    }

    @PatchMapping("/{eventId}/guests/{guestId}/profile")
    @Operation(summary = "Atualizar dados do convidado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do convidado atualizados"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o convidado"),
            @ApiResponse(responseCode = "404", description = "Convidado não encontrado")
    })
    public ResponseEntity<MessageResponse> updateGuestProfile(
            @Parameter(hidden = true) @RequestAttribute("userId") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @Valid @RequestBody UpdateGuestProfileDTO request
    ) {
        guestService.updateGuestProfile(eventId, guestId, userId, request);
        return ResponseEntity.ok(new MessageResponse("Dados do convidado atualizados com sucesso."));
    }

    @PostMapping(value = "/{eventId}/guests/{guestId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar ou substituir a imagem do convidado")
    public ResponseEntity<MessageResponse> uploadGuestImage(
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") UUID userId
    ) {
        guestImageService.uploadImage(eventId, guestId, userId, file);
        return ResponseEntity.ok(new MessageResponse("Imagem do convidado atualizada com sucesso."));
    }

    @GetMapping("/{eventId}/guests/{guestId}/image")
    @Operation(summary = "Buscar a imagem do convidado")
    @SecurityRequirements
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
    @Operation(summary = "Remover a imagem do convidado")
    public ResponseEntity<Void> removeGuestImage(
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @RequestAttribute("userId") UUID userId
    ) {
        boolean removed = guestImageService.removeImage(eventId, guestId, userId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private UUID extractUserId(@Nullable Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
