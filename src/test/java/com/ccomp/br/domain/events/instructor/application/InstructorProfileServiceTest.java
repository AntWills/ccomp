package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.instructor.dto.UpdateInstructorProfileRequest;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfile;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfileRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorProfileServiceTest {
    @Mock private EventCache eventCache;
    @Mock private EventRepository eventRepository;
    @Mock private EventAccessPolicy policy;
    @Mock private UserManagement users;
    @Mock private EventInstructorRepository instructors;
    @Mock private EventInstructorProfileRepository profiles;
    InstructorProfileService service;
    final UUID requester = UUID.randomUUID();
    final long eventId = 4L;
    final long profileId = 8L;
    EventDTO eventDto;
    Event event;
    EventInstructorProfile profile;

    @BeforeEach
    void setup() {
        service = new InstructorProfileService(eventCache, eventRepository, policy, users, instructors, profiles);
        eventDto = mock(EventDTO.class);
        event = mock(Event.class);
        profile = EventInstructorProfile.builder()
                .id(profileId)
                .event(event)
                .name("Teacher")
                .description("Bio")
                .build();
        when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDto));
    }

    @Test
    void listProfilesReturnsProfiles() {
        when(policy.canView(eventDto, requester)).thenReturn(true);
        when(profiles.findAllByEventIdOrderByCreatedAtDescIdDesc(eventId)).thenReturn(List.of(profile));
        when(instructors.findAllByProfile_Id(profileId)).thenReturn(List.of());
        assertThat(service.listProfiles(eventId, requester)).hasSize(1);
    }
    @Test
    void listProfilesRejectsUnauthorizedRequester() {
        when(policy.canView(eventDto, requester)).thenReturn(false);
        assertThatThrownBy(() -> service.listProfiles(eventId, requester)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createProfilePersistsProfile() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(profiles.save(any())).thenAnswer(invocation -> {
            EventInstructorProfile savedProfile = invocation.getArgument(0);
            savedProfile.setId(profileId);
            return savedProfile;
        });
        when(instructors.findAllByProfile_Id(profileId)).thenReturn(List.of());
        assertThat(service.createProfile(eventId, requester, "Teacher", "Bio", null).name()).isEqualTo("Teacher");
    }
    @Test
    void createProfileRejectsUnauthorizedRequester() {
        when(policy.canEdit(eventDto, requester)).thenReturn(false);
        assertThatThrownBy(() -> service.createProfile(eventId, requester, "Teacher", "Bio", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateProfileChangesAndPersistsFields() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.of(profile));
        when(instructors.findAllByProfile_Id(profileId)).thenReturn(List.of());

        assertThat(service.updateProfile(eventId, profileId, requester,
                new UpdateInstructorProfileRequest("Updated", "New bio")).name())
                .isEqualTo("Updated");
        verify(profiles).save(profile);
    }
    @Test
    void updateProfileFailsWhenProfileIsMissing() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProfile(eventId, profileId, requester,
                new UpdateInstructorProfileRequest("Updated", null))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void linkProfileAccountFailsWhenProfileIsMissing() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.linkProfileAccount(eventId, profileId, requester, "a@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void linkProfileAccountRejectsUnauthorizedRequester() {
        when(policy.canEdit(eventDto, requester)).thenReturn(false);
        assertThatThrownBy(() -> service.linkProfileAccount(eventId, profileId, requester, "a@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteProfileRemovesProfileAndItsLinks() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.of(profile));
        when(instructors.findAllByProfile_Id(profileId)).thenReturn(List.of());
        service.deleteProfile(eventId, profileId, requester);
        verify(instructors).deleteAll(List.of());
        verify(profiles).delete(profile);
    }
    @Test
    void deleteProfileFailsWhenProfileIsMissing() {
        when(policy.canEdit(eventDto, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteProfile(eventId, profileId, requester)).isInstanceOf(ResourceNotFoundException.class);
    }
}
