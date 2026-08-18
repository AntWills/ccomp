package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubMemberService;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.enums.ClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.ClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/members")
@Tag(name = "Club Members", description = "Endpoints para gerenciamento de membros e instrutores de clubes")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    public ClubMemberController(ClubMemberService clubMemberService) {
        this.clubMemberService = clubMemberService;
    }

    @PostMapping("/search")
    @Operation(
            summary = "Busca membros de clubes",
            description = "Retorna uma lista paginada de membros de clubes utilizando paginação por cursor (CursorPage). Permite aplicar filtros complexos no corpo da requisição."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<CursorPage<ClubMember>> search(
            @Parameter(description = "Filtros de busca (ex: clube, usuário, papel, status e edição)") 
            @RequestBody(required = false) ClubMemberFilter filter,
            
            @Parameter(description = "Cursor para a próxima página (codificado)") 
            @RequestParam(required = false) String cursor,
            
            @Parameter(description = "Quantidade de itens por página (máximo 50)") 
            @RequestParam(defaultValue = "10") int size
    ) {
        if (filter == null) {
            filter = ClubMemberFilter.builder().build();
        }
        return ResponseEntity.ok(clubMemberService.searchMembers(filter, cursor, size));
    }

    @PostMapping("/enroll/{clubId}")
    @Operation(
            summary = "Inscreve o usuário autenticado",
            description = "Inscreve o usuário atual como membro (MEMBER) em uma edição específica de um clube. Caso o usuário já tenha participado anteriormente, sua inscrição pode ser reativada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscrição realizada ou reativada com sucesso"),
            @ApiResponse(responseCode = "409", description = "Usuário já está matriculado nesta edição do clube ativamente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClubMember> enroll(
            @Parameter(description = "ID do clube", required = true) 
            @PathVariable Long clubId,
            
            @Parameter(description = "Identificador da edição/temporada do clube", required = true) 
            @RequestParam String edition,
            
            @Parameter(hidden = true) 
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(clubMemberService.enrollMember(clubId, userId, edition));
    }

    @PostMapping("/{clubId}/staff/{email}")
    @Operation(
            summary = "Adiciona um instrutor/staff ao clube",
            description = "Adiciona um usuário existente (buscado pelo email) como instrutor (INSTRUCTOR) ou membro em uma edição do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro adicionado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado com o e-mail informado"),
            @ApiResponse(responseCode = "409", description = "Usuário já faz parte da equipe deste clube nesta edição"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClubMember> addMember(
            @Parameter(description = "ID do clube", required = true) 
            @PathVariable Long clubId,
            
            @Parameter(description = "Email do usuário a ser adicionado", required = true) 
            @PathVariable String email,
            
            @Parameter(description = "Papel a ser atribuído (INSTRUCTOR ou MEMBER)", required = true) 
            @RequestParam ClubMemberRole role,
            
            @Parameter(description = "Identificador da edição/temporada (opcional)") 
            @RequestParam(required = false) String edition
    ) {
        return ResponseEntity.ok(clubMemberService.addStaff(clubId, email, role, edition));
    }

    @PatchMapping("/{memberId}/status")
    @Operation(
            summary = "Altera o status de um membro",
            description = "Permite inativar ou ativar a participação de um membro/staff em um clube. Inativar (INACTIVE) irá definir a data de saída (leftAt)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido ou membro não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Void> changeStatus(
            @Parameter(description = "ID da matrícula do membro", required = true) 
            @PathVariable Long memberId,
            
            @Parameter(description = "Novo status (ACTIVE, INACTIVE, PENDING)", required = true) 
            @RequestParam ClubMemberStatus status
    ) {
        clubMemberService.changeMemberStatus(memberId, status);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
