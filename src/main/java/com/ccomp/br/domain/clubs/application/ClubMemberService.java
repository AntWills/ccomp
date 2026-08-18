package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.enums.ClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.ClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberSpec;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
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
    private final UserManagement userManagement;

    public ClubMemberService(ClubMemberRepository clubMemberRepository, UserManagement userManagement) {
        this.clubMemberRepository = clubMemberRepository;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubMember> searchMembers(ClubMemberFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        Specification<ClubMember> spec = ClubMemberSpec.filterByAndCursor(filter,
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

    @Transactional(readOnly = true)
    public List<ClubMember> loadUserHistory(UUID userId) {
        return clubMemberRepository.findByUserId(userId);
    }

    @Transactional
    public ClubMember enrollMember(Long clubId, UUID userId, String edition) {
        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(userId, clubId)
                .stream()
                .filter(m -> m.getRole() == ClubMemberRole.MEMBER && m.getEdition().equals(edition))
                .findFirst();

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == ClubMemberStatus.ACTIVE) {
                throw new ConflictException("Usuário já está matriculado nesta edição do clube.");
            } else {
                existing.setStatus(ClubMemberStatus.ACTIVE);
                existing.setLeftAt(null);
                return clubMemberRepository.save(existing);
            }
        }

        ClubMember member = ClubMember.builder()
                .clubId(clubId)
                .userId(userId)
                .role(ClubMemberRole.MEMBER)
                .status(ClubMemberStatus.ACTIVE)
                .edition(edition)
                .joinedAt(LocalDateTime.now())
                .build();
        return clubMemberRepository.save(member);
    }

    @Transactional
    public ClubMember addStaff(Long clubId, String email, ClubMemberRole role, String edition) {
        UserDTO user = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail informado."));

        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(user.id(), clubId)
                .stream()
                .filter(m -> m.getRole() == role && (edition == null || edition.equals(m.getEdition())))
                .findFirst();

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == ClubMemberStatus.ACTIVE) {
                throw new ConflictException("Usuário já faz parte da equipe deste clube.");
            } else {
                existing.setStatus(ClubMemberStatus.ACTIVE);
                existing.setLeftAt(null);
                return clubMemberRepository.save(existing);
            }
        }

        ClubMember member = ClubMember.builder()
                .clubId(clubId)
                .userId(user.id())
                .role(role)
                .status(ClubMemberStatus.ACTIVE)
                .edition(edition)
                .joinedAt(LocalDateTime.now())
                .build();
        return clubMemberRepository.save(member);
    }

    @Transactional
    public void changeMemberStatus(Long memberId, ClubMemberStatus newStatus) {
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado"));
        member.setStatus(newStatus);
        if (newStatus == ClubMemberStatus.INACTIVE) {
            member.setLeftAt(LocalDateTime.now());
        } else if (newStatus == ClubMemberStatus.ACTIVE) {
            member.setLeftAt(null);
        }
        clubMemberRepository.save(member);
    }
}
