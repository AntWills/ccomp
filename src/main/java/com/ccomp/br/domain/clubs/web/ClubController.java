package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.dto.CreateClubRequestDTO;
import com.ccomp.br.domain.clubs.dto.UpdateClubRequestDTO;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "Club", description = "Endpoints para gerenciamento de clubes")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping("/search")
    @Operation(
            summary = "Lista todos os clubes públicos",
            description = "Retorna uma lista paginada por cursor com os clubes públicos cadastrados. Endpoint público."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")
    })
    @SecurityRequirements
    public ResponseEntity<CursorPage<ClubResponseDTO>> search(
            @Parameter(description = "Cursor para a próxima página (codificado)")
            @RequestParam(required = false) String nextCursor,

            @Parameter(description = "Quantidade de itens por página (máximo 50)")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(clubService.search(nextCursor, pageSize));
    }

    @PostMapping("/me")
    @Operation(
            summary = "Lista os clubes do usuário autenticado",
            description = "Retorna uma lista paginada por cursor dos clubes gerenciados/inscrito pelo usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clubes retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    public ResponseEntity<CursorPage<ClubResponseDTO>> findMyClubs(
            @Parameter(description = "Filtro opcional pelo papel do usuário no clube (INSTRUCTOR ou MEMBER)")
            @RequestParam(required = false) EnumClubMemberRole role,

            @Parameter(description = "Cursor para a próxima página (codificado)")
            @RequestParam(required = false) String nextCursor,

            @Parameter(description = "Quantidade de itens por página")
            @RequestParam(defaultValue = "10") int pageSize,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(clubService.findByUserInvolved(userId, role, nextCursor, pageSize));
    }

    @GetMapping("/{clubId}")
    @Operation(
            summary = "Busca um clube pelo ID",
            description = "Retorna os detalhes de um clube específico. Se o clube não for público, exige que o requisitante seja o instrutor responsável."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube encontrado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para visualizar este clube"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    })
    public ResponseEntity<ClubResponseDTO> findById(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return clubService.findById(clubId, extractUserId(jwt))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo clube",
            description = "Cria um clube vinculado ao usuário autenticado, tornando-o o instrutor do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Clube criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para criar clubes")
    })
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
            summary = "Atualiza dados de um clube",
            description = "Atualiza as informações do clube especificado. Restrito ao instrutor do clube ou administrador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — usuário não é instrutor do clube"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ClubResponseDTO> update(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @Valid @RequestBody UpdateClubRequestDTO dto,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        return ResponseEntity.ok(clubService.update(clubId, dto, requesterId));
    }

    @DeleteMapping("/{clubId}")
    @Operation(
            summary = "Remove um clube",
            description = "Remove o clube especificado. Restrito ao instrutor do clube ou administrador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Clube removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — usuário não é instrutor do clube"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);
        clubService.delete(clubId, requesterId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return jwt != null ? UUID.fromString(jwt.getSubject()) : null;
    }
}