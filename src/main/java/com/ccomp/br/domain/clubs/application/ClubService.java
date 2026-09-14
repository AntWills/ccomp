package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.dto.*;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.Club;
import com.ccomp.br.domain.clubs.persistence.ClubDslRepository;
import com.ccomp.br.domain.clubs.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.util.ClubMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubService {
    private final ClubRepository clubRepository;
    private final ClubDslRepository clubDslRepository;
    private final ClubMemberService clubMemberService;
    private final ClubAccessPolicy clubAccessPolicy;
    private final ClubMapper clubMapper;
    private final UserManagement userManagement;

    public ClubService(ClubRepository clubRepository, ClubDslRepository clubDslRepository, ClubMemberService clubMemberService, ClubAccessPolicy clubAccessPolicy, ClubMapper clubMapper, UserManagement userManagement) {
        this.clubRepository = clubRepository;
        this.clubDslRepository = clubDslRepository;
        this.clubMemberService = clubMemberService;
        this.clubAccessPolicy = clubAccessPolicy;
        this.clubMapper = clubMapper;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubResponseDTO> search(String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);

        ClubPublishedCursor cursorDecoded = CursorUtils.decode(cursor, ClubPublishedCursor.class);
        List<ClubResponseDTO> results = clubDslRepository
                .findAllPublishedWithCursor(cursorDecoded, finalPageSize + 1);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                cr -> new ClubPublishedCursor(cr.id(), cr.publishedAt())
        );
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubResponseDTO> findByUserInvolved(UUID userId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);

        ClubCreatedCursor cursorDecoded = CursorUtils.decode(cursor, ClubCreatedCursor.class);
        List<ClubResponseDTO> results = clubDslRepository
                .findByUserInvolvedWithCursor(userId, cursorDecoded, finalPageSize + 1);

        return CursorUtils.buildPage(
                results,
                finalPageSize,
                cr -> new ClubCreatedCursor(cr.id(), cr.createdAt())
        );
    }

    @Transactional(readOnly = true)
    public Optional<ClubResponseDTO> findById(Long clubId, UUID userId) {
        return clubRepository.findById(clubId)
                .filter(club -> club.isPublic() || clubAccessPolicy.isInstructor(clubId, userId))
                .map(clubMapper::toDTO);
    }

    @Transactional
    public ClubResponseDTO create(CreateClubRequestDTO dto, UUID instructorId) {
        UserDTO userDTO = userManagement.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        if (!userDTO.isTeamMember())
            throw new AccessDeniedException("Apenas membros da equipe (STAFF, MODERATOR ou ADMIN) podem criar clubes.");


        Club club = Club.builder()
                .name(dto.name())
                .summary(dto.summary())
//                .instructor(instructorId)
                .build();
        Club saved = clubRepository.save(club);

        clubMemberService.addMember(saved.getId(), userDTO.id(), EnumClubMemberRole.INSTRUCTOR);

        return clubMapper.toDTO(saved);
    }

    @Transactional
    public ClubResponseDTO update(Long clubId, UpdateClubRequestDTO dto, UUID userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Clube não encontrado com o id:" + clubId));

        UserDTO userDTO = userManagement.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        boolean canEdit = userDTO.isAdmin()
                || clubAccessPolicy.isInstructor(clubId, userId);

        if (!canEdit)
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        clubMapper.updateEntityFromDto(dto, club);

        clubRepository.save(club);

        return clubMapper.toDTO(club);
    }

    @Transactional
    public void delete(Long clubId, UUID userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Clube não encontrado com o id:" + clubId));

        UserDTO userDTO = userManagement.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        boolean canEdit = userDTO.isAdmin()
                || clubAccessPolicy.isInstructor(clubId, userId);

        if (!canEdit)
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        clubRepository.delete(club);
    }
}
