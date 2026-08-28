package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberSpec;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClubMemberService {
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAccessPolicy clubAccessPolicy;
    private final UserManagement userManagement;

    public ClubMemberService(ClubRepository clubRepository, ClubMemberRepository clubMemberRepository, ClubAccessPolicy clubAccessPolicy, UserManagement userManagement) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.clubAccessPolicy = clubAccessPolicy;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubMemberListItem> searchMembers(UUID userId, Long clubId, ClubMemberFilter filter, String cursor, int pageSize) {
        boolean canAccess = clubAccessPolicy.isInstructor(clubId, userId)
                || SecurityUtils.isAdmin();

        if (!canAccess)
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        if (pageSize > 50) pageSize = 50;

        Specification<ClubMember> spec = ClubMemberSpec.filterByAndCursor(clubId, filter,
                CursorUtils.decode(cursor, LocalDateTime.class).orElse(null));

        int finalPageSize = pageSize;
        List<ClubMember> results = clubMemberRepository.findBy(spec, query -> query
                .limit(finalPageSize + 1)
                .sortBy(Sort.by(Sort.Direction.DESC, "joinedAt"))
                .all());

        boolean hasNext = results.size() > finalPageSize;
        List<ClubMember> page = hasNext ? results.subList(0, finalPageSize) : results;

        List<UUID> ids = page.stream()
                .map(ClubMember::getUserId)
                .toList();

        Map<UUID, UserSummaryView> userMap = userManagement.findAllSummaryByIds(ids)
                .stream()
                .collect(Collectors.toMap(UserSummaryView::getId, Function.identity(), (user1, user2) -> user1));

        String nextCursor = hasNext && !page.isEmpty() ? CursorUtils.encode(page.getLast().getJoinedAt()) : null;

        List<ClubMemberListItem> contents = page.stream()
                .map(cm -> ClubMemberListItem.builder()
                        .id(cm.getId())
                        .clubId(cm.getClub().getId())
                        .user(userMap.get(cm.getUserId()))
                        .role(cm.getRole())
                        .status(cm.getStatus())
                        .joinedAt(cm.getJoinedAt())
                        .leftAt(cm.getLeftAt())
                        .build())
                .toList();

        return new CursorPage<>(contents, nextCursor, null);
    }

//    @Transactional(readOnly = true)
//    public List<ClubMember> loadUserHistory(UUID userId) {
//        return clubMemberRepository.findByUserId(userId);
//    }

    @Transactional
    public ClubMember enrollMember(Long clubId, UUID userId) {
        if(!clubRepository.existsById(clubId))
            throw new ResourceNotFoundException("Clube não encontrado com o id:" + clubId);

        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(userId, clubId);

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == EnumClubMemberStatus.ACTIVE)
                return existing;

            existing.activate();
            existing.setRole(EnumClubMemberRole.MEMBER);
            return clubMemberRepository.save(existing);
        }

        ClubMember member = ClubMember.builder()
                .club(clubRepository.getReferenceById(clubId))
                .userId(userId)
                .role(EnumClubMemberRole.MEMBER)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        return clubMemberRepository.save(member);
    }

    @Transactional
    public MessageResponse unenrollMember(Long clubId, UUID userId) {
        if(!clubRepository.existsById(clubId))
            throw new ResourceNotFoundException("Clube não encontrado com o id:" + clubId);

        ClubMember member = clubMemberRepository.findByUserIdAndClubId(userId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("O usuário não está vinculado ao clube."));

        if(!member.isStillLinked())
            return new MessageResponse("O usuário não está vinculado a este clube.");

        member.unsubscribe();

        clubMemberRepository.save(member);

        return new MessageResponse("Inscrição cancelada com sucesso.");
    }

    @Transactional
    public ClubMember addMemberByEmail(UUID userLoggedId, Long clubId, String email, EnumClubMemberRole role) {
        UserDTO user = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail informado."));

        boolean canAccess = clubAccessPolicy.isInstructor(clubId, userLoggedId)
                || SecurityUtils.isAdmin();

        if (!canAccess)
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        return addMember(clubId, user.id(), role);
    }

    @Transactional
    public ClubMember addMember(Long clubId, UUID userId, EnumClubMemberRole role) {
        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(userId, clubId);

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == EnumClubMemberStatus.ACTIVE && existing.getRole() == role)
                throw new ConflictException("Usuário já faz parte da equipe deste clube.");

            existing.activate();
            existing.setRole(role);
            return clubMemberRepository.save(existing);
        }

        ClubMember member = ClubMember.builder()
                .club(clubRepository.getReferenceById(clubId))
                .userId(userId)
                .role(role)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        return clubMemberRepository.save(member);
    }

    @Transactional
    public void changeMemberStatus(UUID userLoggedId, Long clubId, Long memberId, EnumClubMemberStatus newStatus) {
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));

        if(member.getUserId().equals(userLoggedId) && !SecurityUtils.isAdmin())
            throw new ConflictException("O usuário não pode alterar o próprio status.");

        boolean canAccess = member.isInstructor(clubId)
                || SecurityUtils.isAdmin();

        if (!canAccess)
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        if (newStatus == EnumClubMemberStatus.INACTIVE) {
            member.deactivate();
        } else if (newStatus == EnumClubMemberStatus.ACTIVE) {
            member.activate();
        }
        clubMemberRepository.save(member);
    }
}
