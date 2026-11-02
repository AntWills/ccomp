package com.ccomp.br.domain.events.activities.materials.web;

import com.ccomp.br.domain.events.activities.materials.application.ActivityMaterialsService;
import com.ccomp.br.domain.events.materials.dto.CreateMaterialLinkRequest;
import com.ccomp.br.domain.events.materials.dto.MaterialResponse;
import com.ccomp.br.domain.storage.external.StorageExternal;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/activities/{activityId}/materials")
@Tag(name = "events/activities/materials (Materiais da atividade)")
public class ActivityMaterialsController {
    private final ActivityMaterialsService service;
    public ActivityMaterialsController(ActivityMaterialsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> list(@PathVariable Long activityId, @AuthenticationPrincipal @Nullable Jwt jwt) {
        return ResponseEntity.ok(service.list(activityId, userId(jwt)));
    }
    @PostMapping("/links")
    public ResponseEntity<MaterialResponse> addLink(@PathVariable Long activityId, @Valid @RequestBody CreateMaterialLinkRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(service.addLink(activityId, requireUser(jwt), request));
    }
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(@PathVariable Long activityId, @RequestParam String title,
            @RequestParam(required = false) String description, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(service.upload(activityId, requireUser(jwt), title, description, file));
    }
    @GetMapping("/{materialId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long activityId, @PathVariable Long materialId, @AuthenticationPrincipal @Nullable Jwt jwt) {
        Optional<StorageExternal.StoredFile> stored = service.getFile(activityId, materialId, userId(jwt));
        return stored.map(file -> {
            MediaType type = file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(file.contentType());
            return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options", "nosniff").body(file.resource());
        }).orElseGet(() -> ResponseEntity.notFound().<Resource>build());
    }
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> delete(@PathVariable Long activityId, @PathVariable Long materialId, @AuthenticationPrincipal Jwt jwt) {
        service.delete(activityId, materialId, requireUser(jwt)); return ResponseEntity.noContent().build();
    }
    private UUID userId(@Nullable Jwt jwt) { return jwt == null ? null : UUID.fromString(jwt.getSubject()); }
    private UUID requireUser(@Nullable Jwt jwt) { if (jwt == null) throw new com.ccomp.br.shared.exceptions.UserNotFoundException("O usuário precisa estar autenticado."); return userId(jwt); }
}
