package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.Club;
import com.ccomp.br.domain.clubs.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberDslRepository;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceTest {
    @Mock ClubRepository clubRepository;
    @Mock ClubMemberRepository memberRepository;
    @Mock ClubMemberDslRepository memberDslRepository;
    @Mock ClubAccessPolicy accessPolicy;
    @Mock com.ccomp.br.domain.users.external.UserManagement userManagement;
    @InjectMocks ClubMemberService service;

    @Test
    void enrollReturnsExistingActiveMembershipWithoutSaving() {
        UUID userId = UUID.randomUUID();
        ClubMember active = ClubMember.builder().club(Club.builder().id(2L).build()).userId(userId)
                .status(EnumClubMemberStatus.ACTIVE).role(EnumClubMemberRole.MEMBER).build();
        when(clubRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(active));

        assertThat(service.enrollMember(2L, userId)).isSameAs(active);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void enrollReactivatesPreviousMembershipAndResetsRole() {
        UUID userId = UUID.randomUUID();
        ClubMember previous = ClubMember.builder().club(Club.builder().id(2L).build()).userId(userId)
                .status(EnumClubMemberStatus.CANCELLED).role(EnumClubMemberRole.INSTRUCTOR).build();
        when(clubRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(previous));
        when(memberRepository.save(previous)).thenReturn(previous);

        service.enrollMember(2L, userId);

        assertThat(previous.getStatus()).isEqualTo(EnumClubMemberStatus.ACTIVE);
        assertThat(previous.getRole()).isEqualTo(EnumClubMemberRole.MEMBER);
    }

    @Test
    void enrollRequiresExistingClub() {
        when(clubRepository.existsById(404L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.enrollMember(404L, UUID.randomUUID()));
    }

    @Test
    void addingSameActiveRoleConflicts() {
        UUID userId = UUID.randomUUID();
        ClubMember member = ClubMember.builder().status(EnumClubMemberStatus.ACTIVE)
                .role(EnumClubMemberRole.MEMBER).build();
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(member));

        assertThrows(ConflictException.class,
                () -> service.addMember(2L, userId, EnumClubMemberRole.MEMBER));
    }

    @Test
    void memberCannotChangeOwnStatus() {
        UUID userId = UUID.randomUUID();
        ClubMember member = ClubMember.builder().id(7L).club(Club.builder().id(2L).build()).userId(userId)
                .status(EnumClubMemberStatus.ACTIVE).role(EnumClubMemberRole.MEMBER).build();
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThrows(ConflictException.class, () -> service.changeMemberStatus(
                    userId, 2L, 7L, EnumClubMemberStatus.INACTIVE));
        }
        verify(memberRepository, never()).save(any());
    }

    @Test
    void memberSearchRequiresInstructorOrModerator() {
        UUID userId = UUID.randomUUID();
        when(accessPolicy.isInstructor(2L, userId)).thenReturn(false);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThrows(AccessDeniedException.class,
                    () -> service.searchMembers(userId, 2L, new ClubMemberFilter(null, null), null, 10));
        }
        verifyNoInteractions(memberDslRepository);
    }
}
