package com.ccomp.br.domain.clubs.members.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.clubs.members.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.core.persistence.Club;
import com.ccomp.br.domain.clubs.core.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.core.enums.EnumClubStatus;
import com.ccomp.br.domain.clubs.members.persistence.ClubMember;
import com.ccomp.br.domain.clubs.members.persistence.ClubMemberDslRepository;
import com.ccomp.br.domain.clubs.members.persistence.ClubMemberRepository;
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
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(Club.builder().id(2L).status(EnumClubStatus.OPEN).build()));
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(active));

        assertThat(service.enrollMember(2L, userId)).isSameAs(active);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void enrollReactivatesPreviousMembershipAndResetsRole() {
        UUID userId = UUID.randomUUID();
        ClubMember previous = ClubMember.builder().club(Club.builder().id(2L).build()).userId(userId)
                .status(EnumClubMemberStatus.CANCELLED).role(EnumClubMemberRole.INSTRUCTOR).build();
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(Club.builder().id(2L).status(EnumClubStatus.OPEN).build()));
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(previous));
        when(memberRepository.save(previous)).thenReturn(previous);

        service.enrollMember(2L, userId);

        assertThat(previous.getStatus()).isEqualTo(EnumClubMemberStatus.ACTIVE);
        assertThat(previous.getRole()).isEqualTo(EnumClubMemberRole.MEMBER);
    }

    @Test
    void enrollRequiresExistingClub() {
        when(clubRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.enrollMember(404L, UUID.randomUUID()));
    }

    @Test
    void enrollRejectsClubThatIsNotOpen() {
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(
                Club.builder().id(2L).status(EnumClubStatus.MEMBERS_ONLY).build()));
        assertThrows(ConflictException.class, () -> service.enrollMember(2L, UUID.randomUUID()));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void enrollEnforcesCapacityForActiveMembers() {
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(
                Club.builder().id(2L).status(EnumClubStatus.OPEN).maxMembers(1L).build()));
        when(memberRepository.findByUserIdAndClubId(any(), eq(2L))).thenReturn(Optional.empty());
        when(memberRepository.countByClub_IdAndRoleAndStatus(2L, EnumClubMemberRole.MEMBER,
                EnumClubMemberStatus.ACTIVE)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.enrollMember(2L, UUID.randomUUID()));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addInstructorDoesNotConsultMemberCapacity() {
        when(clubRepository.getReferenceById(2L)).thenReturn(Club.builder().id(2L).maxMembers(0L).build());
        when(memberRepository.findByUserIdAndClubId(any(), eq(2L))).thenReturn(Optional.empty());
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClubMember result = service.addMember(2L, UUID.randomUUID(), EnumClubMemberRole.INSTRUCTOR);

        assertThat(result.getRole()).isEqualTo(EnumClubMemberRole.INSTRUCTOR);
        verify(memberRepository, never()).countByClub_IdAndRoleAndStatus(any(), any(), any());
    }

    @Test
    void reactivatingMemberEnforcesCapacityButInstructorStatusDoesNot() {
        UUID managerId = UUID.randomUUID();
        ClubMember member = ClubMember.builder().id(7L).club(Club.builder().id(2L).build())
                .userId(UUID.randomUUID()).status(EnumClubMemberStatus.BANNED).role(EnumClubMemberRole.MEMBER).build();
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(accessPolicy.isInstructor(2L, managerId)).thenReturn(true);
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(Club.builder().id(2L).maxMembers(1L).build()));
        when(memberRepository.countByClub_IdAndRoleAndStatus(2L, EnumClubMemberRole.MEMBER,
                EnumClubMemberStatus.ACTIVE)).thenReturn(1L);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThrows(ConflictException.class, () -> service.changeMemberStatus(
                    managerId, 2L, 7L, EnumClubMemberStatus.ACTIVE));
        }
        verify(memberRepository, never()).save(any());
    }

    @Test
    void managerCanBanMemberAndBannedMembersAreNotReenrolled() {
        UUID managerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ClubMember member = ClubMember.builder().id(7L).club(Club.builder().id(2L).build())
                .userId(memberId).status(EnumClubMemberStatus.ACTIVE).role(EnumClubMemberRole.MEMBER).build();
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(accessPolicy.isInstructor(2L, managerId)).thenReturn(true);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            service.changeMemberStatus(managerId, 2L, 7L, EnumClubMemberStatus.BANNED);
        }
        assertThat(member.getStatus()).isEqualTo(EnumClubMemberStatus.BANNED);
        verify(memberRepository).save(member);

        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(Club.builder().id(2L).status(EnumClubStatus.OPEN).build()));
        when(memberRepository.findByUserIdAndClubId(memberId, 2L)).thenReturn(Optional.of(member));
        assertThrows(ConflictException.class, () -> service.enrollMember(2L, memberId));
    }

    @Test
    void addingSameActiveRoleConflicts() {
        UUID userId = UUID.randomUUID();
        ClubMember member = ClubMember.builder().status(EnumClubMemberStatus.ACTIVE)
                .role(EnumClubMemberRole.MEMBER).build();
        when(memberRepository.findByUserIdAndClubId(userId, 2L)).thenReturn(Optional.of(member));
        when(clubRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(Club.builder().id(2L).build()));

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
