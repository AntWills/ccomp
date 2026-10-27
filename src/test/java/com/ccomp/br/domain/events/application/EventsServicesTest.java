package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.CreateEventDTO;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.dto.EventCursor;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.dto.EventSearchResult;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.core.dto.UpdateEventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventDslRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.utils.CursorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

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

    @Mock private EventCache eventCache;
    @Mock private EventDslRepository eventDslRepository;
    @Mock private EventAccessPolicy eventAccessPolicy;

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

    @Test
    @DisplayName("Busca eventos repassando searchTerm e inclui a relevância no cursor seguinte")
    void searchEventsWithFilters_preservesSearchRelevanceInNextCursor() {
        var filter = new EventsFilterRequest(
                EnumEventCategory.ACADEMIC_EDUCATIONAL,
                EnumEventFormat.IN_PERSON,
                "computing"
        );
        var first = new EventListItemDTO(10L, "Computing Summit", "computing-summit", null, null,
                EnumEventCategory.ACADEMIC_EDUCATIONAL, EnumEventFormat.IN_PERSON, EnumEventStatus.PUBLISHED,
                null, null, null, false, LocalDateTime.parse("2026-11-10T10:00:00"), null, null, null);
        var second = new EventListItemDTO(9L, "Computing Forum", "computing-forum", null, null,
                EnumEventCategory.ACADEMIC_EDUCATIONAL, EnumEventFormat.IN_PERSON, EnumEventStatus.PUBLISHED,
                null, null, null, false, LocalDateTime.parse("2026-11-09T10:00:00"), null, null, null);
        when(eventDslRepository.findByCursor(filter, null, 2)).thenReturn(List.of(
                new EventSearchResult(first, 1.8), new EventSearchResult(second, 1.4)));

        var page = eventsServices.searchEventsWithFilters(filter, null, 1);

        assertThat(page.content()).containsExactly(first);
        EventCursor nextCursor = CursorUtils.decode(page.nextCursor(), EventCursor.class);
        assertThat(nextCursor).isEqualTo(new EventCursor(1.8, first.startDate(), first.id()));
        verify(eventDslRepository).findByCursor(filter, null, 2);
    }

    @Test
    @DisplayName("Busca sem searchTerm conserva o cursor de data e ID sem score de relevância")
    void searchEventsWithFiltersOmitsRelevanceFromCursorWithoutSearchTerm() {
        var filter = new EventsFilterRequest(null, null);
        var first = new EventListItemDTO(5L, "Evento", "evento", null, null,
                EnumEventCategory.ACADEMIC_EDUCATIONAL, EnumEventFormat.IN_PERSON, EnumEventStatus.PUBLISHED,
                null, null, null, false, LocalDateTime.parse("2026-11-10T10:00:00"), null, null, null);
        var second = new EventListItemDTO(4L, "Outro evento", "outro-evento", null, null,
                EnumEventCategory.ACADEMIC_EDUCATIONAL, EnumEventFormat.IN_PERSON, EnumEventStatus.PUBLISHED,
                null, null, null, false, LocalDateTime.parse("2026-11-09T10:00:00"), null, null, null);
        when(eventDslRepository.findByCursor(filter, null, 2)).thenReturn(List.of(
                new EventSearchResult(first, null), new EventSearchResult(second, null)));

        var page = eventsServices.searchEventsWithFilters(filter, null, 1);

        assertThat(page.content()).containsExactly(first);
        EventCursor nextCursor = CursorUtils.decode(page.nextCursor(), EventCursor.class);
        assertThat(nextCursor).isEqualTo(new EventCursor(null, first.startDate(), first.id()));
        verify(eventDslRepository).findByCursor(filter, null, 2);
    }

    @Test
    @DisplayName("Limita pageSize de busca ao máximo permitido e decodifica cursor")
    void searchEventsWithFiltersCapsPageSizeAndPassesDecodedCursor() {
        var filter = new EventsFilterRequest(null, null);
        var cursor = new EventCursor(LocalDateTime.parse("2026-11-10T10:00:00"), 44L);
        String encodedCursor = CursorUtils.encode(cursor);
        when(eventDslRepository.findByCursor(filter, cursor, 51)).thenReturn(List.of());

        var page = eventsServices.searchEventsWithFilters(filter, encodedCursor, 100);

        assertThat(page.content()).isEmpty();
        verify(eventDslRepository).findByCursor(filter, cursor, 51);
    }

    @Test
    @DisplayName("Busca eventos do dono e propaga cursor e limite ao DSL")
    void findAllByOwnerIdDelegatesWithDecodedCursorAndExtraRow() {
        var cursor = new EventCursor(LocalDateTime.parse("2026-11-10T10:00:00"), 44L);
        String encodedCursor = CursorUtils.encode(cursor);
        when(eventDslRepository.findAllByOwnerId(userId, cursor, 3)).thenReturn(List.of());

        var page = eventsServices.findAllByOwnerId(userId, encodedCursor, 2);

        assertThat(page.content()).isEmpty();
        verify(eventDslRepository).findAllByOwnerId(userId, cursor, 3);
    }

    @Test
    @DisplayName("Busca inscrições do participante e eventos editáveis delegando cursores ao DSL")
    void subscriptionsAndEditableSearchesDelegateToDsl() {
        var cursor = new EventCursor(LocalDateTime.parse("2026-11-10T10:00:00"), 44L);
        String encodedCursor = CursorUtils.encode(cursor);
        when(eventDslRepository.findAllSubscriptions(userId, cursor, 3)).thenReturn(List.of());
        when(eventDslRepository.findAllWhereUserIsEditor(userId, cursor, 3)).thenReturn(List.of());

        assertThat(eventsServices.findAllSubscriptions(userId, encodedCursor, 2).content()).isEmpty();
        assertThat(eventsServices.findMyEditableEvents(userId, encodedCursor, 2).content()).isEmpty();

        verify(eventDslRepository).findAllSubscriptions(userId, cursor, 3);
        verify(eventDslRepository).findAllWhereUserIsEditor(userId, cursor, 3);
    }

    @Nested
    @DisplayName("Get By Id - Consulta por ID")
    class GetById {

        @Test
        @DisplayName("Retorna o evento quando é publicamente acessível")
        void getById_returnsEventDTO_whenEventIsPubliclyAccessible() {
            when(eventCache.findById(eventId)).thenReturn(Optional.of(expectedEventDTO));
            when(eventAccessPolicy.canView(expectedEventDTO, userId)).thenReturn(true);

            Optional<EventDTO> result = eventsServices.getById(eventId, userId);

            assertThat(result).isPresent().contains(expectedEventDTO);
            verify(eventCache).findById(eventId);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando evento é privado e usuário não é dono nem editor")
        void getById_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(eventCache.findById(eventId)).thenReturn(Optional.of(expectedEventDTO));
            when(eventAccessPolicy.canView(expectedEventDTO, userId)).thenReturn(false);

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

            when(eventCache.findBySlug(slug)).thenReturn(Optional.of(expectedEventDTO));
            when(expectedEventDTO.isPublished()).thenReturn(true);

            Optional<EventDTO> result = eventsServices.getBySlug(slug);

            assertThat(result).isPresent().contains(expectedEventDTO);
        }

        @Test
        @DisplayName("Retorna Optional.empty() quando o evento existe mas não está publicado")
        void getBySlug_returnsEmpty_whenEventIsNotPublished() {
            String slug = "semana-de-tecnologia-a1b2c3";

            when(eventCache.findBySlug(slug)).thenReturn(Optional.of(expectedEventDTO));
            when(expectedEventDTO.isPublished()).thenReturn(false);

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
            verify(eventCache).evict(existingEvent);
        }

        @Test
        @DisplayName("Lança UserNotFoundException quando o responsável não existe")
        void create_throwsWhenOwnerDoesNotExist() {
            when(userManagement.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventsServices.create(userId,
                    new CreateEventDTO("Evento teste", null, null, null, null)))
                    .isInstanceOf(com.ccomp.br.shared.exceptions.UserNotFoundException.class)
                    .hasMessage("Usuário responsável não encontrado no sistema.");

            verifyNoInteractions(eventRepository, eventCache);
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
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(true);

            EventDTO result = eventsServices.update(request, eventId, userId);

            assertThat(result).isNotNull().isEqualTo(expectedEventDTO);
            verify(eventMapper).updateEntityFromDto(request, existingEvent);
            verify(eventRepository).save(existingEvent);
        }

        @Test
        @DisplayName("Atualiza título e regenera slug quando título foi informado")
        void updateRegeneratesSlugWhenTitleChanges() {
            UpdateEventDTO request = mock(UpdateEventDTO.class);
            when(request.titleOpt()).thenReturn(Optional.of("Novo título"));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventRepository.existsBySlug(anyString())).thenReturn(false);
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(true);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);

            eventsServices.update(request, eventId, userId);

            verify(existingEvent).setTitle("Novo título");
            verify(existingEvent).setSlug(argThat(slug -> slug.startsWith("novo-titulo-")));
            verify(eventMapper).updateEntityFromDto(request, existingEvent);
            verify(eventCache).evict(existingEvent);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException ao atualizar evento inexistente")
        void updateThrowsWhenEventDoesNotExist() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventsServices.update(mock(UpdateEventDTO.class), eventId, userId))
                    .isInstanceOf(com.ccomp.br.shared.exceptions.ResourceNotFoundException.class)
                    .hasMessage("Evento não encontrado.");

            verifyNoInteractions(eventMapper, eventAccessPolicy, eventCache);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não é dono nem editor do evento")
        void update_throwsAccessDeniedException_whenUserHasNoPermission() {
            UpdateEventDTO request = mock(UpdateEventDTO.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(false);

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
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(true);
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
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(false);

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
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(true);

            eventsServices.delete(eventId, userId);

            verify(eventRepository).delete(existingEvent);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando um editor tenta remover o evento (apenas dono/admin pode)")
        void delete_throwsAccessDeniedException_whenUserIsNotOwner() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.eventToEventDTO(existingEvent)).thenReturn(expectedEventDTO);
            when(eventAccessPolicy.canEdit(expectedEventDTO, userId)).thenReturn(false);

            assertThatThrownBy(() -> eventsServices.delete(eventId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para remover este evento.");

            verify(eventRepository, never()).delete(any(Event.class));
        }
    }
}
