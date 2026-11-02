package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubMemberService;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "club/members (Membros)", description = "Endpoints para gerenciamento de membros e instrutores de clubes")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    public ClubMemberController(ClubMemberService clubMemberService) {
        this.clubMemberService = clubMemberService;
    }

    @PostMapping("/{clubId}/members/search")
    @Operation(
            summary = "Busca membros de um clube",
            description = "Consulta os membros do clube com filtros de papel e status enviados no corpo. Retorna uma página por cursor. Requer usuário autenticado que seja instrutor ativo do clube ou MODERATOR/ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros ou parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é instrutor ativo do clube nem MODERATOR/ADMIN")
    })
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
            description = "Vincula o usuário autenticado ao clube com papel MEMBER. Uma inscrição ativa existente é retornada sem alteração; vínculos anteriores são reativados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscrição realizada ou reativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado")
    })
    public ResponseEntity<ClubMember> enroll(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(clubMemberService.enrollMember(clubId, userId));
    }

    @DeleteMapping("/{clubId}/members/unenroll")
    @Operation(
            summary = "Cancela a inscrição do usuário autenticado",
            description = "Altera o vínculo do usuário autenticado para CANCELLED. Se já estiver cancelado, retorna uma mensagem sem alterar o registro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscrição cancelada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado"),
            @ApiResponse(responseCode = "400", description = "Usuário não possui vínculo com o clube")
    })
    public ResponseEntity<MessageResponse> unenroll(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(clubMemberService.unenrollMember(clubId, userId));
    }

    @PostMapping("/{clubId}/members/staff/{email}")
    @Operation(
            summary = "Adiciona um membro ou instrutor por e-mail",
            description = "Busca um usuário cadastrado pelo e-mail e o vincula ao clube com o papel informado (INSTRUCTOR ou MEMBER). Permitido ao instrutor ativo do clube ou a usuários MODERATOR/ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é instrutor ativo do clube nem MODERATOR/ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Usuário já tem vínculo ativo com o mesmo papel")
    })
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

    @PatchMapping("/{clubId}/members/{memberId}/status")
    @Operation(
            summary = "Altera o status de um membro no clube",
            description = "Atualiza o status do vínculo (ACTIVE, INACTIVE ou CANCELLED). Permitido ao instrutor do clube ou a usuários MODERATOR/ADMIN. O próprio membro não pode alterar o status, exceto MODERATOR/ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é instrutor ativo do clube nem MODERATOR/ADMIN"),
            @ApiResponse(responseCode = "409", description = "O membro não pode alterar o próprio status"),
            @ApiResponse(responseCode = "404", description = "Registro de membro não encontrado")
    })
    public ResponseEntity<Void> changeStatus(
            @Parameter(description = "ID do clube", required = true)
            @PathVariable Long clubId,

            @Parameter(description = "ID da matrícula do membro", required = true)
            @PathVariable Long memberId,

            @Parameter(description = "Novo status (ACTIVE, INACTIVE ou CANCELLED)", required = true)
            @RequestParam EnumClubMemberStatus status,

            @AuthenticationPrincipal Jwt jwt
    ) {
        clubMemberService.changeMemberStatus(extractUserId(jwt), clubId, memberId, status);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Jwt jwt) {
        return jwt != null ? UUID.fromString(jwt.getSubject()) : null;
    }
}
