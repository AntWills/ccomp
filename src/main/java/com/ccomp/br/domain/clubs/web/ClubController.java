package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.CreateClubRequestDTO;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.dto.UpdateClubRequestDTO;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "Clubs", description = "Endpoints para gerenciamento de clubes")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping("search")
    @Operation(
            summary = "Lista todos os clubes",
            description = "Retorna a lista com o cursor de todos os clubes cadastrados. Não requer que o usuário esteja logado."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @SecurityRequirements
    public ResponseEntity<CursorPage<ClubResponseDTO>> search (
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(clubService.search(nextCursor, pageSize));
    }

    @PostMapping("/me")
    @Operation(
            summary = "Lista os clubes do usuário autenticado",
            description = "Retorna a lista paginada por cursor (ordenada por data de criação) dos clubes criados pelo próprio usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Lista de clubes retornada com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    public ResponseEntity<CursorPage<ClubResponseDTO>> findMyClubs(
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID instructorId = extractUserId(jwt);
        return ResponseEntity.ok(clubService.findByInstructor(instructorId, nextCursor, pageSize));
    }

    @GetMapping("/{clubId}")
    @Operation(
            summary = "Busca um clube pelo ID",
            description = "Retorna os dados detalhados de um clube específico caso ele esteja público ou caso o usuário seja o instrutor do clube."
    )
    @ApiResponse(responseCode = "200", description = "Clube encontrado")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    public ResponseEntity<ClubResponseDTO> findById(
            @Parameter(description = "ID do clube") @PathVariable Long clubId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return clubService.findById(clubId, extractUserId(jwt))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo clube",
            description = "Cria um clube vinculado ao usuário autenticado, que se torna automaticamente o instrutor (dono) do clube."
    )
    @ApiResponse(responseCode = "201", description = "Clube criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ClubResponseDTO> create(
            @Valid @RequestBody CreateClubRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID instructorId = extractUserId(jwt);
        ClubResponseDTO response = clubService.create(dto, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{clubId}")
    @Operation(
            summary = "Atualiza um clube",
            description = "Atualiza os dados de um clube existente. Apenas o instrutor (dono) do clube pode realizar esta operação."
    )
    @ApiResponse(responseCode = "200", description = "Clube atualizado com sucesso")
    @ApiResponse(responseCode = "403", description = "Usuário não é o dono do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ClubResponseDTO> update(
            @Parameter(description = "ID do clube") @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        return ResponseEntity.ok(clubService.update(clubId, dto, requesterId));
    }

    @DeleteMapping("/{clubId}")
    @Operation(
            summary = "Remove um clube",
            description = "Remove um clube existente. Apenas o instrutor (dono) do clube pode realizar esta operação."
    )
    @ApiResponse(responseCode = "204", description = "Clube removido com sucesso")
    @ApiResponse(responseCode = "403", description = "Usuário não é o dono do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do clube") @PathVariable Long clubId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        clubService.delete(clubId, requesterId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}