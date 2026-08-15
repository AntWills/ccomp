package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.CreateClubRequestDTO;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.dto.UpdateClubRequestDTO;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    @Operation(
            summary = "Cria um novo clube",
            description = "Cria um clube vinculado ao usuário autenticado, que se torna automaticamente o instrutor (dono) do clube."
    )
    @ApiResponse(responseCode = "201", description = "Clube criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    public ResponseEntity<ClubResponseDTO> create(
            @Valid @RequestBody CreateClubRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID instructorId = extractUserId(jwt);
        ClubResponseDTO response = clubService.create(dto, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Lista todos os clubes",
            description = "Retorna a lista com o cursor de todos os clubes cadastrados. Não requer que o usuário esteja logado."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<CursorPage<ClubResponseDTO>> search (
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(clubService.search(nextCursor, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um clube pelo ID",
            description = "Retorna os dados detalhados de um clube específico."
    )
    @ApiResponse(responseCode = "200", description = "Clube encontrado")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    public ResponseEntity<ClubResponseDTO> findById(
            @Parameter(description = "ID do clube") @PathVariable Long id
    ) {
        return clubService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Atualiza um clube",
            description = "Atualiza os dados de um clube existente. Apenas o instrutor (dono) do clube pode realizar esta operação."
    )
    @ApiResponse(responseCode = "200", description = "Clube atualizado com sucesso")
    @ApiResponse(responseCode = "403", description = "Usuário não é o dono do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    public ResponseEntity<ClubResponseDTO> update(
            @Parameter(description = "ID do clube") @PathVariable Long id,
            @Valid @RequestBody UpdateClubRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        return ResponseEntity.ok(clubService.update(id, dto, requesterId));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove um clube",
            description = "Remove um clube existente. Apenas o instrutor (dono) do clube pode realizar esta operação."
    )
    @ApiResponse(responseCode = "204", description = "Clube removido com sucesso")
    @ApiResponse(responseCode = "403", description = "Usuário não é o dono do clube")
    @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do clube") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        clubService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}