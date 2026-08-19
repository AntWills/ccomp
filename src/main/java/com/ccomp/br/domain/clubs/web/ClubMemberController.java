package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubMemberService;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "Club Members", description = "Endpoints para gerenciamento de membros e instrutores de clubes")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    public ClubMemberController(ClubMemberService clubMemberService) {
        this.clubMemberService = clubMemberService;
    }

    @PostMapping("/{clubId}/members/search")
    @Operation(
            summary = "Busca membros de um clube",
            description = "Retorna uma lista paginada por cursor (`CursorPage`) com os membros do clube, permitindo filtrar por papel (role) e status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros ou parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — apenas instrutores do clube ou administradores"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<CursorPage<ClubMemberListItem>> search(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @Valid @RequestBody ClubMemberFilter filter,

            @Parameter(description = "Cursor para a próxima página (codificado)")
            @RequestParam(required = false) String cursor,

            @Parameter(description = "Quantidade de itens por página (máximo 50)")
            @RequestParam(defaultValue = "10") int size,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(clubMemberService.searchMembers(extractUserId(jwt), clubId, filter, cursor, size));
    }

    @PostMapping("/{clubId}/members/enroll")
    @Operation(
            summary = "Inscreve o usuário autenticado em um clube",
            description = "Inscreve o usuário logado como participante (`MEMBER`) no clube. Reativa a inscrição caso o usuário tenha participado anteriormente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscrição realizada ou reativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado"),
            @ApiResponse(responseCode = "409", description = "Usuário já possui inscrição ativa neste clube")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ResponseEntity<ClubMember> enroll(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(clubMemberService.enrollMember(clubId, userId));
    }

    @PostMapping("/{clubId}/members/staff/{email}")
    @Operation(
            summary = "Adiciona um membro ou instrutor por e-mail",
            description = "Busca um usuário cadastrado pelo e-mail e o vincula ao clube com o papel informado (`INSTRUCTOR` ou `MEMBER`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — apenas instrutores do clube ou administradores"),
            @ApiResponse(responseCode = "404", description = "Usuário ou clube não encontrado"),
            @ApiResponse(responseCode = "409", description = "Usuário já faz parte deste clube")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ClubMember> addMember(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @Parameter(description = "E-mail do usuário a ser adicionado", required = true)
            @PathVariable String email,

            @Parameter(description = "Papel a ser atribuído (INSTRUCTOR ou MEMBER)", required = true)
            @RequestParam EnumClubMemberRole role,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(clubMemberService.addMemberByEmail(extractUserId(jwt), clubId, email, role));
    }

    @PatchMapping("/members/{memberId}/status")
    @Operation(
            summary = "Altera o status de um membro no clube",
            description = "Atualiza o status de participação (`ACTIVE`, `INACTIVE`, `PENDING`). Ao inativar (`INACTIVE`), a data de saída (`leftAt`) é preenchida automaticamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — apenas instrutores do clube ou administradores"),
            @ApiResponse(responseCode = "404", description = "Registro de membro não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> changeStatus(
            @Parameter(description = "ID da matrícula do membro", required = true)
            @PathVariable Long memberId,

            @Parameter(description = "Novo status (ACTIVE, INACTIVE, PENDING)", required = true)
            @RequestParam EnumClubMemberStatus status
    ) {
        clubMemberService.changeMemberStatus(memberId, status);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return jwt != null ? UUID.fromString(jwt.getSubject()) : null;
    }
}