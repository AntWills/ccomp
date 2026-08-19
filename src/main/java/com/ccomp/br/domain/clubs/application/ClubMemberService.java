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
                .filter(m -> m.getRole() == EnumClubMemberRole.MEMBER)
                .findFirst();

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == EnumClubMemberStatus.ACTIVE) {
                throw new ConflictException("Usuário já está matriculado nesta edição do clube.");
            } else {
                existing.setStatus(EnumClubMemberStatus.ACTIVE);
                existing.setLeftAt(null);
                return clubMemberRepository.save(existing);
            }
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
    public ClubMember addStaff(Long clubId, String email, EnumClubMemberRole role) {
        UserDTO user = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail informado."));

        Optional<ClubMember> existingMemberOpt = clubMemberRepository.findByUserIdAndClubId(user.id(), clubId)
                .stream()
                .filter(m -> m.getRole() == role )
                .findFirst();

        if (existingMemberOpt.isPresent()) {
            ClubMember existing = existingMemberOpt.get();
            if (existing.getStatus() == EnumClubMemberStatus.ACTIVE) {
                throw new ConflictException("Usuário já faz parte da equipe deste clube.");
            } else {
                existing.setStatus(EnumClubMemberStatus.ACTIVE);
                existing.setLeftAt(null);
                return clubMemberRepository.save(existing);
            }
        }

        ClubMember member = ClubMember.builder()
                .clubId(clubId)
                .userId(user.id())
                .role(role)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        return clubMemberRepository.save(member);
    }

    @Transactional
    public void changeMemberStatus(Long memberId, EnumClubMemberStatus newStatus) {
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado"));
        member.setStatus(newStatus);
        if (newStatus == EnumClubMemberStatus.INACTIVE) {
            member.setLeftAt(LocalDateTime.now());
        } else if (newStatus == EnumClubMemberStatus.ACTIVE) {
            member.setLeftAt(null);
        }
        clubMemberRepository.save(member);
    }
}
