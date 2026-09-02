package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.activities.ActivityDTO;
import com.ccomp.br.domain.events.dto.activities.CreateActivityDTO;
import com.ccomp.br.domain.events.dto.activities.UpdateActivityDTO;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;

import com.ccomp.br.domain.events.util.ActivityMapper;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServicesTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EditorServices editorServices;

    @Mock
    private EventActivityRepository activityRepository;

    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private ActivitiesServices activityServices;

    private UUID userId;
    private Long eventId;
    private Long activityId;
    private Event existingEvent;
    private EventActivity existingActivity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = 1L;
        activityId = 10L;
        existingEvent = mock(Event.class);
        existingActivity = mock(EventActivity.class);
    }

    @Nested
    @DisplayName("Create Activity - Criar Atividade")
    class CreateActivity {

        @Test
        @DisplayName("Cria atividade com sucesso quando o usuário tem permissão (é editor)")
        void createActivity_returnsActivityDTO_whenUserIsOwner() {
            CreateActivityDTO request = new CreateActivityDTO("Palestra Spring Boot", "Descrição da palestra");
            ActivityDTO expectedDTO = mock(ActivityDTO.class);
            EventActivity savedActivity = mock(EventActivity.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(true);
            when(activityRepository.save(any(EventActivity.class))).thenReturn(savedActivity);
            when(activityMapper.eventActivityToActivityDTO(savedActivity)).thenReturn(expectedDTO);

            ActivityDTO response = activityServices.createActivity(userId, eventId, request);

            assertThat(response).isNotNull().isEqualTo(expectedDTO);
            verify(activityRepository).save(any(EventActivity.class));
            verify(activityMapper).eventActivityToActivityDTO(savedActivity);
        }

        @Test
        @DisplayName("Cria atividade com sucesso quando o usuário tem permissão (é editor)")
        void createActivity_returnsActivityDTO_whenUserHasPermission() {
            CreateActivityDTO request = new CreateActivityDTO("Palestra Spring Boot", "Descrição da palestra");
            ActivityDTO expectedDTO = mock(ActivityDTO.class);
            EventActivity savedActivity = mock(EventActivity.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(true);
            when(activityRepository.save(any(EventActivity.class))).thenReturn(savedActivity);
            when(activityMapper.eventActivityToActivityDTO(savedActivity)).thenReturn(expectedDTO);

            ActivityDTO response = activityServices.createActivity(userId, eventId, request);

            assertThat(response).isNotNull().isEqualTo(expectedDTO);
            verify(activityRepository).save(any(EventActivity.class));
            verify(activityMapper).eventActivityToActivityDTO(savedActivity);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não é dono nem editor do evento")
        void createActivity_throwsAccessDeniedException_whenUserHasNoPermission() {
            CreateActivityDTO request = new CreateActivityDTO("Palestra Spring Boot", "Descrição da palestra");

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityServices.createActivity(userId, eventId, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O usuario não tem acesso a este recurso.");

            verify(activityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Activity - Atualizar Atividade")
    class UpdateActivity {

        @Test
        @DisplayName("Atualiza atividade com sucesso quando o usuário tem permissão")
        void updateActivity_returnsUpdatedActivityDTO_whenUserHasPermission() {
            UpdateActivityDTO request = new UpdateActivityDTO("Novo Título", "Nova Descrição", null);
            ActivityDTO expectedDTO = mock(ActivityDTO.class);

            when(existingActivity.getEvent()).thenReturn(existingEvent);
            when(existingEvent.isOwner(userId)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));
//            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(true);
            when(activityRepository.save(existingActivity)).thenReturn(existingActivity);
            when(activityMapper.eventActivityToActivityDTO(existingActivity)).thenReturn(expectedDTO);

            ActivityDTO response = activityServices.updateActivity(userId, activityId, request);

            assertThat(response).isNotNull().isEqualTo(expectedDTO);
            verify(activityMapper).updateEventActivityFromRequest(request, existingActivity);
            verify(activityRepository).save(existingActivity);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando a atividade não existe")
        void updateActivity_throwsResourceNotFoundException_whenActivityDoesNotExist() {
            UpdateActivityDTO request = new UpdateActivityDTO("Novo Título", "Nova Descrição", null);

            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityServices.updateActivity(userId, activityId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Atividade não existe.");

            verify(activityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Activity - Remover Atividade")
    class DeleteActivity {

        @Test
        @DisplayName("Remove atividade com sucesso quando o usuário é editor/dono do evento")
        void deleteActivity_deletesSuccessfully_whenUserHasPermission() {
            when(existingActivity.getEvent()).thenReturn(existingEvent);
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));

            activityServices.deleteActivity(userId, activityId);

            verify(activityRepository).deleteById(activityId);
        }

        @Test
        @DisplayName("Lança AccessDeniedException ao tentar remover atividade sem ter permissão")
        void deleteActivity_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(existingActivity.getEvent()).thenReturn(existingEvent);
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(false);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));

            assertThatThrownBy(() -> activityServices.deleteActivity(userId, activityId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O usuario não tem acesso a este recurso.");

            verify(activityRepository, never()).deleteById(anyLong());
        }
    }
}