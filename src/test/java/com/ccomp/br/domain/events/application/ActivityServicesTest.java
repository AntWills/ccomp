package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.activities.application.ActivitiesServices;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.CreateActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;

import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
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
    private EventAccessPolicy eventAccessPolicy;
    @Mock private EventCache eventCache;
    @Mock private EventMapper eventMapper;

    @Mock
    private EventActivityRepository activityRepository;

    @Mock
    private EventActivityDslRepository activityDslRepository;

    @Mock
    private ActivityCache activityCache;

    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private ActivitiesServices activityServices;

    private UUID userId;
    private Long eventId;
    private Long activityId;
    private Event existingEvent;
    private EventActivity existingActivity;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = 1L;
        activityId = 10L;
        existingEvent = mock(Event.class);
        existingActivity = mock(EventActivity.class);
        eventDTO = mock(EventDTO.class);

    }

    @Nested
    @DisplayName("Create Activity - Criar Atividade")
    class CreateActivity {

        @Test
        @DisplayName("Cria atividade com sucesso quando o usuário tem permissão (é editor)")
        void createActivity_returnsActivityDTO_whenUserHasPermission() {
            CreateActivityDTO request = new CreateActivityDTO("Palestra Spring Boot", "Descrição da palestra");
            ActivityDTO expectedDTO = mock(ActivityDTO.class);
            EventActivity savedActivity = mock(EventActivity.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(eventDTO);
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
            when(activityRepository.save(any(EventActivity.class))).thenReturn(savedActivity);
            when(activityMapper.eventActivityToActivityDTO(savedActivity)).thenReturn(expectedDTO);

            ActivityDTO response = activityServices.createActivity(userId, eventId, request);

            assertThat(response).isNotNull().isEqualTo(expectedDTO);
            verify(activityRepository).save(any(EventActivity.class));
            verify(activityMapper).eventActivityToActivityDTO(savedActivity);
            verify(activityCache).evictEventLists();
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não é dono nem editor do evento")
        void createActivity_throwsAccessDeniedException_whenUserHasNoPermission() {
            CreateActivityDTO request = new CreateActivityDTO("Palestra Spring Boot", "Descrição da palestra");

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(eventDTO);
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityServices.createActivity(userId, eventId, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O usuario não tem acesso a este recurso.");

            verify(activityRepository, never()).save(any());
        }

        @Test
        void createActivity_throwsNotFound_whenEventDoesNotExist() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityServices.createActivity(userId, eventId,
                    new CreateActivityDTO("Palestra", "Descrição")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Evento não encontrado.");
            verify(activityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Activity - Atualizar Atividade")
    class UpdateActivity {

        @Test
        @DisplayName("Atualiza atividade com sucesso quando o usuário tem permissão")
        void updateActivity_returnsUpdatedActivityDTO_whenUserHasPermission() {
            UpdateActivityDTO request = UpdateActivityDTO.builder()
                    .title("Novo titulo")
                    .description("nova descrição")
                    .build();

            ActivityDTO expectedDTO = mock(ActivityDTO.class);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));
            when(existingActivity.getEventId()).thenReturn(eventId);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
            when(activityRepository.save(existingActivity)).thenReturn(existingActivity);
            when(activityMapper.eventActivityToActivityDTO(existingActivity)).thenReturn(expectedDTO);

            ActivityDTO response = activityServices.updateActivity(userId, activityId, request);

            assertThat(response).isNotNull().isEqualTo(expectedDTO);
            verify(activityMapper).updateEventActivityFromRequest(request, existingActivity);
            verify(activityRepository).save(existingActivity);
            verify(activityCache).evict(existingActivity);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando a atividade não existe")
        void updateActivity_throwsResourceNotFoundException_whenActivityDoesNotExist() {
            UpdateActivityDTO request = UpdateActivityDTO.builder()
                    .title("Novo titulo")
                    .description("nova descrição")
                    .build();

            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityServices.updateActivity(userId, activityId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Atividade não existe.");

            verify(activityRepository, never()).save(any());
        }

        @Test
        void updateActivity_throwsAccessDenied_whenUserCannotEditEvent() {
            UpdateActivityDTO request = UpdateActivityDTO.builder().title("Novo título").build();
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));
            when(existingActivity.getEventId()).thenReturn(eventId);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityServices.updateActivity(userId, activityId, request))
                    .isInstanceOf(AccessDeniedException.class);
            verify(activityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Activity - Remover Atividade")
    class DeleteActivity {

        @Test
        @DisplayName("Remove atividade com sucesso quando o usuário é editor/dono do evento")
        void deleteActivity_deletesSuccessfully_whenUserHasPermission() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));
            when(existingActivity.getEventId()).thenReturn(eventId);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);

            activityServices.deleteActivity(userId, activityId);

            verify(activityRepository).deleteById(activityId);
            verify(activityCache).evict(existingActivity);
        }

        @Test
        @DisplayName("Lança AccessDeniedException ao tentar remover atividade sem ter permissão")
        void deleteActivity_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(existingActivity));
            when(existingActivity.getEventId()).thenReturn(eventId);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityServices.deleteActivity(userId, activityId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O usuario não tem acesso a este recurso.");

            verify(activityRepository, never()).deleteById(anyLong());
        }

        @Test
        void deleteActivity_throwsNotFound_whenActivityDoesNotExist() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityServices.deleteActivity(userId, activityId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Atividade não existe.");
            verify(activityRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Search Activities - Busca paginada")
    class SearchActivities {
        @Test
        void searchByCursor_returnsPage_whenUserCanViewEvent() {
            EventActivityDTO activity = new EventActivityDTO(activityId, eventId, "Palestra", null, 1L,
                    null, null, null, null, null, null, null);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canView(eventDTO, userId)).thenReturn(true);
            when(activityCache.findAllByEventIdWithCursor(eventId, null, 51)).thenReturn(java.util.List.of(activity));

            var page = activityServices.searchByCursor(eventId, null, userId);

            assertThat(page.content()).containsExactly(activity);
            assertThat(page.nextCursor()).isNull();
            verify(activityCache).findAllByEventIdWithCursor(eventId, null, 51);
        }

        @Test
        void searchByCursor_throwsNotFound_whenEventDoesNotExist() {
            when(eventCache.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityServices.searchByCursor(eventId, null, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Evento não encontrado.");
            verifyNoInteractions(activityCache);
        }

        @Test
        void searchByCursor_throwsAccessDenied_whenUserCannotViewEvent() {
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTO));
            when(eventAccessPolicy.canView(eventDTO, userId)).thenReturn(false);

            assertThatThrownBy(() -> activityServices.searchByCursor(eventId, null, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(activityCache);
        }
    }
}
