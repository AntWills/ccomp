package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberSpec;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorCodec;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubAccessPolicy clubAccessPolicy;
    private final UserManagement userManagement;

    public ClubMemberService(ClubMemberRepository clubMemberRepository, ClubAccessPolicy clubAccessPolicy, UserManagement userManagement) {
        this.clubMemberRepository = clubMemberRepository;
        this.clubAccessPolicy = clubAccessPolicy;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubMember> searchMembers(UUID userId, Long clubId, ClubMemberFilter filter, String cursor, int pageSize) {
        if(!clubAccessPolicy.isInstructor(clubId, userId)) {
            var user = userManagement.findById(userId)
                    .orElseThrow(() ->
                            new AccessDeniedException("O usuário não tem acesso a este recurso."));

            if (!user.isAdmin()) {
                throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
            }
        }

        if(pageSize > 50) pageSize = 50;

        Specification<ClubMember> spec = ClubMemberSpec.filterByAndCursor(clubId, filter,
                CursorCodec.decode(cursor, LocalDateTime.class).orElse(null));

        int finalPageSize = pageSize;
        List<ClubMember> results = clubMemberRepository.findBy(spec, query -> query
                .limit(finalPageSize + 1)
                .sortBy(Sort.by(Sort.Direction.DESC, "joinedAt"))
                .all());

        boolean hasNext = results.size() > finalPageSize;
        List<ClubMember> page = hasNext ? results.subList(0, finalPageSize) : results;
        String nextCursor = hasNext && !page.isEmpty() ? CursorCodec.encode(page.getLast().getJoinedAt()) : null;

        return new CursorPage<>(page, nextCursor, null);
    }

//    @Transactional(readOnly = true)
//    public List<ClubMember> loadUserHistory(UUID userId) {
//        return clubMemberRepository.findByUserId(userId);
//    }

    @Transactional
    public ClubMember enrollMember(Long clubId, UUID userId) {
        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(userId, clubId);

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == EnumClubMemberStatus.ACTIVE)
                throw new ConflictException("Usuário já está matriculado nesta edição do clube.");

            existing.activate();
            existing.setRole(EnumClubMemberRole.MEMBER);
            return clubMemberRepository.save(existing);
        }

        ClubMember member = ClubMember.builder()
                .clubId(clubId)
                .userId(userId)
                .role(EnumClubMemberRole.MEMBER)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        return clubMemberRepository.save(member);
    }
    @Transactional
    public ClubMember addMemberByEmail(Long clubId, String email, EnumClubMemberRole role) {
        UserDTO user = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail informado."));

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
                .clubId(clubId)
                .userId(userId)
                .role(role)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        return clubMemberRepository.save(member);
    }

    @Transactional
    public void changeMemberStatus(Long memberId, EnumClubMemberStatus newStatus) {
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));

        if (newStatus == EnumClubMemberStatus.INACTIVE) {
            member.deactivate();
        } else if (newStatus == EnumClubMemberStatus.ACTIVE) {
            member.activate();
        }
        clubMemberRepository.save(member);
    }
}
