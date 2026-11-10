package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.dto.CreateClubRequestDTO;
import com.ccomp.br.domain.clubs.dto.UpdateClubRequestDTO;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.Club;
import com.ccomp.br.domain.clubs.persistence.ClubDslRepository;
import com.ccomp.br.domain.clubs.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.utils.ClubMapper;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {
    @Mock ClubRepository clubRepository;
    @Mock ClubDslRepository clubDslRepository;
    @Mock ClubMemberService clubMemberService;
    @Mock ClubAccessPolicy accessPolicy;
    @Mock ClubMapper mapper;
    @Mock UserManagement userManagement;
    @InjectMocks ClubService service;

    private UserDTO user(UUID id, EnumRoles role) {
        return new UserDTO(id, "User", null, EnumUserStatusAccount.ACTIVE, null, null, role, null);
    }

    @Test
    void createRequiresTeamRoleAndAddsCreatorAsInstructor() {
        UUID id = UUID.randomUUID();
        UserDTO user = user(id, EnumRoles.STAFF);
        Club saved = Club.builder().id(9L).name("Club").build();
        when(userManagement.findById(id)).thenReturn(Optional.of(user));
        when(clubRepository.save(any(Club.class))).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(null);

        service.create(new CreateClubRequestDTO("Club", "Summary"), id);

        verify(clubMemberService).addMember(9L, id, EnumClubMemberRole.INSTRUCTOR);
        verify(clubRepository).save(argThat(club -> club.getName().equals("Club")
                && club.getSummary().equals("Summary")));
    }

    @Test
    void createRejectsNonTeamRole() {
        UUID id = UUID.randomUUID();
        when(userManagement.findById(id)).thenReturn(Optional.of(user(id, EnumRoles.USER)));

        assertThrows(AccessDeniedException.class,
                () -> service.create(new CreateClubRequestDTO("Club", null), id));
        verifyNoInteractions(clubRepository, clubMemberService);
    }

    @Test
    void findByIdHidesUnpublishedClubFromNonInstructor() {
        UUID id = UUID.randomUUID();
        when(clubRepository.findById(9L)).thenReturn(Optional.of(Club.builder().id(9L).build()));
        when(accessPolicy.isInstructor(9L, id)).thenReturn(false);

        assertThat(service.findById(9L, id)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void updateRequiresExistingClubAndEditorPermission() {
        UUID id = UUID.randomUUID();
        when(clubRepository.findById(9L)).thenReturn(Optional.of(Club.builder().id(9L).build()));
        when(userManagement.findById(id)).thenReturn(Optional.of(user(id, EnumRoles.USER)));
        when(accessPolicy.isInstructor(9L, id)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.update(9L, new UpdateClubRequestDTO(null, null, null, null, null), id));
        verify(clubRepository, never()).save(any());
    }

    @Test
    void deleteMissingClubThrowsNotFound() {
        when(clubRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(4L, UUID.randomUUID()));
    }
}
