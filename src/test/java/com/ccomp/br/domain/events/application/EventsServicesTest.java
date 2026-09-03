package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.events.CreateEventDTO;
import com.ccomp.br.domain.events.dto.events.EventDTO;
import com.ccomp.br.domain.events.dto.events.UpdateEventDTO;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventBlaze;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventsServicesTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserManagement userManagement;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EditorServices editorServices;

    @Mock
    private EventBlaze eventBlaze;

    @InjectMocks
    private EventsServices eventsServices;

    private UUID userId;
    private Long eventId;
    private Event existingEvent;
    private EventDTO expectedEventDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = 1L;
        existingEvent = mock(Event.class);
        expectedEventDTO = mock(EventDTO.class);
    }

    @Nested
    @DisplayName("Get By Id - Consulta por ID")
    class GetById {

        @Test
        @DisplayName("Retorna o evento quando é publicamente acessível")
        void getById_returnsEventDTO_whenEventIsPubliclyAccessible() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isPubliclyAccessible()).thenReturn(true);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);

            Optional<EventDTO> result = eventsServices.getById(eventId, userId);

            assertThat(result).isPresent().contains(expectedEventDTO);
            verify(eventRepository).findById(eventId);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando evento é privado e usuário não é dono nem editor")
        void getById_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isPubliclyAccessible()).thenReturn(false);
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.getById(eventId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não possui permissão para visualizar este evento.");
        }
    }

    @Nested
    @DisplayName("Get By Slug - Consulta por Slug")
    class GetBySlug {

        @Test
        @DisplayName("Retorna o evento quando encontrado e publicado")
        void getBySlug_returnsEventDTO_whenEventIsPublished() {
            String slug = "semana-de-tecnologia-a1b2c3";

            when(eventRepository.findBySlug(slug)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isPublished()).thenReturn(true);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);

            Optional<EventDTO> result = eventsServices.getBySlug(slug);

            assertThat(result).isPresent().contains(expectedEventDTO);
        }

        @Test
        @DisplayName("Retorna Optional.empty() quando o evento existe mas não está publicado")
        void getBySlug_returnsEmpty_whenEventIsNotPublished() {
            String slug = "semana-de-tecnologia-a1b2c3";

            when(eventRepository.findBySlug(slug)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isPublished()).thenReturn(false);

            Optional<EventDTO> result = eventsServices.getBySlug(slug);

            assertThat(result).isEmpty();
            verify(eventMapper, never()).eventToEventDTO(any());
        }
    }

    @Nested
    @DisplayName("Create Event - Criação de Evento")
    class Create {

        @Test
        @DisplayName("Cria evento como rascunho quando o usuário é membro da equipe (STAFF/MODERATOR/ADMIN)")
        void create_createsEventSuccessfully_whenUserIsTeamMember() {
            CreateEventDTO dto = mock(CreateEventDTO.class);
            UserDTO userDTO = mock(UserDTO.class);

            when(dto.title()).thenReturn("I Encontro de Computação");
            when(dto.optionalStartDate()).thenReturn(Optional.empty());
            when(dto.optionalEndDate()).thenReturn(Optional.empty());

            when(userManagement.findById(userId)).thenReturn(Optional.of(userDTO));
            when(userDTO.isTeamMember()).thenReturn(true);
            when(eventRepository.existsBySlug(anyString())).thenReturn(false);
            when(eventRepository.save(any(Event.class))).thenReturn(existingEvent);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);

            EventDTO result = eventsServices.create(userId, dto);

            assertThat(result).isNotNull().isEqualTo(expectedEventDTO);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não é membro da equipe")
        void create_throwsAccessDeniedException_whenUserIsNotTeamMember() {
            CreateEventDTO dto = mock(CreateEventDTO.class);
            UserDTO userDTO = mock(UserDTO.class);

            when(userManagement.findById(userId)).thenReturn(Optional.of(userDTO));
            when(userDTO.isTeamMember()).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.create(userId, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Apenas membros da equipe (STAFF, MODERATOR ou ADMIN) podem criar novos eventos.");

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Event - Atualização de Evento")
    class Update {

        @Test
        @DisplayName("Atualiza o evento com sucesso quando o usuário tem permissão")
        void update_updatesEventSuccessfully_whenUserHasPermission() {
            UpdateEventDTO request = mock(UpdateEventDTO.class);
            when(request.titleOpt()).thenReturn(Optional.empty());

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(true);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);

            EventDTO result = eventsServices.update(request, eventId, userId);

            assertThat(result).isNotNull().isEqualTo(expectedEventDTO);
            verify(eventMapper).updateEntityFromDto(request, existingEvent);
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não é dono nem editor do evento")
        void update_throwsAccessDeniedException_whenUserHasNoPermission() {
            UpdateEventDTO request = mock(UpdateEventDTO.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.update(request, eventId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para alterar o status deste evento.");

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Status - Transição de Status do Evento")
    class UpdateEventStatus {

        @Test
        @DisplayName("Altera status do evento para PUBLISHED e chama o método de domínio publish()")
        void updateEventStatus_changesStatusToPublished_whenUserHasPermission() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(true);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);

            MessageResponse response = eventsServices.updateEventStatus(eventId, EnumEventStatus.PUBLISHED, userId);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Status do evento alterado para: PUBLISHED");

            verify(existingEvent).publish();
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando usuário não possui permissão para alterar o status")
        void updateEventStatus_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(false);
            when(editorServices.hasPermissionEdit(existingEvent, userId)).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.updateEventStatus(eventId, EnumEventStatus.CANCELED, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para alterar o status deste evento.");

            verify(existingEvent, never()).cancel();
            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Event - Remoção de Evento")
    class Delete {

        @Test
        @DisplayName("Remove o evento com sucesso quando o usuário é o dono")
        void delete_removesEventSuccessfully_whenUserIsOwner() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(true);

            eventsServices.delete(eventId, userId);

            verify(eventRepository).delete(existingEvent);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando um editor tenta remover o evento (apenas dono/admin pode)")
        void delete_throwsAccessDeniedException_whenUserIsNotOwner() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(userId)).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.delete(eventId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para remover este evento.");

            verify(eventRepository, never()).delete(any(Event.class));
        }
    }
}