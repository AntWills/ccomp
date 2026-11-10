package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubAccessPolicyTest {
    @Mock ClubMemberRepository repository;
    @InjectMocks ClubAccessPolicy policy;

    @Test
    void instructorAccessRequiresAnActiveInstructorRole() {
        UUID userId = UUID.randomUUID();
        when(repository.existsByUserIdAndClubIdAndRoleAndStatus(userId, 12L,
                EnumClubMemberRole.INSTRUCTOR, EnumClubMemberStatus.ACTIVE)).thenReturn(true);

        assertThat(policy.isInstructor(12L, userId)).isTrue();
        verify(repository).existsByUserIdAndClubIdAndRoleAndStatus(userId, 12L,
                EnumClubMemberRole.INSTRUCTOR, EnumClubMemberStatus.ACTIVE);
    }
}
