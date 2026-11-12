package com.ccomp.br.domain.events.instructor.application;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitationRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitation;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorInvitationStatus;
import com.ccomp.br.domain.events.instructor.dto.InstructorInvitationDTO;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfileRepository;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.domain.users.external.UserManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class InstructorInvitationServiceTest {
    @Mock private ActivityCache activityCache;
    @Mock private EventActivityRepository activityRepository;
    @Mock private EventInstructorRepository instructorRepository;
    @Mock private InstructorInvitationRepository invitationRepository;
    @Mock private EventCache eventCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private UserManagement userManagement;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private EventRepository eventRepository;
    @Mock private EventInstructorProfileRepository profileRepository;

    private InstructorInvitationService service() {
        return new InstructorInvitationService(activityCache, activityRepository, instructorRepository,
                invitationRepository, eventCache, eventAccessPolicy, userManagement, rabbitTemplate,
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC), eventRepository, profileRepository);
    }

    @Test
    void createsAssistantInstructorWithoutPlatformAccount() {
        UUID managerId = UUID.randomUUID();
        long activityId = 42L;
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(7L);
        when(eventCache.findById(7L)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canEdit(event, managerId)).thenReturn(true);
        when(instructorRepository.save(any(EventInstructor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = new InstructorInvitationService(
                activityCache, activityRepository, instructorRepository, invitationRepository,
                eventCache, eventAccessPolicy, userManagement, rabbitTemplate, java.time.Clock.systemUTC()
        ).addInstructor(managerId, activityId, "Auxiliar", "Apoio de sala",
                EnumInstructorRole.ASSISTANT, null);

        assertThat(response.response()).isEqualTo("Ministrante adicionado à atividade.");
        verify(instructorRepository).save(argThat(instructor ->
                instructor.getActivityId().equals(activityId)
                        && instructor.getUserId() == null
                        && instructor.getName().equals("Auxiliar")
                        && instructor.getRole() == EnumInstructorRole.ASSISTANT));
        verifyNoInteractions(userManagement, rabbitTemplate);
    }

    @Test void inviteSendsInvitationForManageableActivity() {
        UUID manager = UUID.randomUUID(); long activityId=42L; EmailAddress email=new EmailAddress("person@example.com");
        EventActivity activity=mock(EventActivity.class); EventDTO event=mock(EventDTO.class);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity)); when(activity.getEventId()).thenReturn(7L);
        when(eventCache.findById(7L)).thenReturn(Optional.of(event)); when(eventAccessPolicy.canEdit(event,manager)).thenReturn(true);
        when(userManagement.findByEmailAddress(email)).thenReturn(Optional.empty());
        when(invitationRepository.findFirstByEmailAddressAndActivity_IdOrderByInvitedAtDesc(email,activityId)).thenReturn(Optional.empty());
        assertThat(service().invite(manager,activityId,email).response()).contains("enviado");
        verify(invitationRepository).save(any(InstructorInvitation.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
    @Test void inviteRejectsUnauthorizedRequester() {
        UUID manager=UUID.randomUUID(); EventActivity activity=mock(EventActivity.class); EventDTO event=mock(EventDTO.class);
        when(activityRepository.findById(42L)).thenReturn(Optional.of(activity)); when(activity.getEventId()).thenReturn(7L);
        when(eventCache.findById(7L)).thenReturn(Optional.of(event)); when(eventAccessPolicy.canEdit(event,manager)).thenReturn(false);
        assertThatThrownBy(() -> service().invite(manager,42L,new EmailAddress("person@example.com"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test void addInstructorRejectsMissingActivity() {
        when(activityRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().addInstructor(UUID.randomUUID(),42L,"Name",null,null,null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void linkInstructorAccountRejectsMissingInstructor() {
        UUID manager=UUID.randomUUID(); EventActivity activity=mock(EventActivity.class); EventDTO event=mock(EventDTO.class);
        when(activityRepository.findById(42L)).thenReturn(Optional.of(activity)); when(activity.getEventId()).thenReturn(7L);
        when(eventCache.findById(7L)).thenReturn(Optional.of(event)); when(eventAccessPolicy.canEdit(event,manager)).thenReturn(true);
        when(instructorRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().linkInstructorAccount(manager,42L,8L,"person@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void replyRejectsUnknownInvitationCode() {
        UUID code=UUID.randomUUID(); when(invitationRepository.findByCode(code)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().reply(code,UUID.randomUUID(),true)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void searchInvitationsRejectsMissingActivity() {
        when(activityCache.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().searchInvitations(UUID.randomUUID(),42L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void cancelRejectsUnknownInvitation() {
        when(invitationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().cancel(UUID.randomUUID(),9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
