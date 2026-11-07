package com.ccomp.br.domain.events.instructor.application;

import java.util.Optional;
import java.util.UUID;

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
import com.ccomp.br.domain.users.external.UserManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                eventCache, eventAccessPolicy, userManagement, rabbitTemplate
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
}
