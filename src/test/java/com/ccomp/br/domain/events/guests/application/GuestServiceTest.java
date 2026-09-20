package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private EventGuestRepository eventGuestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventActivityRepository activityRepository;

    @Mock
    private EventEditorRepository editorRepository;

    @Mock
    private EventGuestDslRepository eventGuestDslRepository;

    @Mock
    private ActivityGuestDslRepository activityGuestDslRepository;

    @InjectMocks
    private GuestService guestService;

    private Long eventId;
    private Long activityId;
    private UUID requesterId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        activityId = 10L;
        requesterId = UUID.randomUUID();
        event = mock(Event.class);
    }

    @Nested
    @DisplayName("Search Event Guests - Consulta Paginada de Convidados do Evento")
    class SearchEventGuests {

        @Test
        @DisplayName("Retorna a lista paginada de convidados quando o evento está publicado")
        void searchEventGuests_returnsCursorPage_whenEventIsPublished() {
            try (MockedStatic<CursorUtils> cursorUtilsMock = mockStatic(CursorUtils.class)) {
                GuestCursor decodedCursor = mock(GuestCursor.class);
                EventGuest guest = mock(EventGuest.class);
                CursorPage<EventGuest> expectedPage = mock(CursorPage.class);

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(event.isOwner(requesterId)).thenReturn(false);
                when(editorRepository.existsByEventIdAndUserId(event.getId(), requesterId)).thenReturn(false);
                when(event.isPublished()).thenReturn(true);

                cursorUtilsMock.when(() -> CursorUtils.decode(any(), eq(GuestCursor.class))).thenReturn(decodedCursor);
                when(eventGuestDslRepository.findGuestsWithCursor(eventId, false, decodedCursor, 20)).thenReturn(List.of(guest));
                cursorUtilsMock.when(() -> CursorUtils.buildPage(anyList(), anyInt(), any())).thenReturn(expectedPage);

                CursorPage<EventGuest> result = guestService.searchEventGuests(eventId, requesterId, "cursorRaw", 20);

                assertThat(result).isNotNull().isEqualTo(expectedPage);
                verify(eventGuestDslRepository).findGuestsWithCursor(eventId, false, decodedCursor, 20);
            }
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o evento não está publicado e o solicitante não é gerente")
        void searchEventGuests_throwsAccessDeniedException_whenEventNotPublishedAndNotManager() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(event.isOwner(requesterId)).thenReturn(false);
            when(editorRepository.existsByEventIdAndUserId(event.getId(), requesterId)).thenReturn(false);
            when(event.isPublished()).thenReturn(false);

            assertThatThrownBy(() -> guestService.searchEventGuests(eventId, requesterId, "cursorRaw", 20))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O evento não está publicado.");

            verify(eventGuestDslRepository, never()).findGuestsWithCursor(any(), anyBoolean(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("Search Activity Guests - Consulta Paginada de Convidados da Atividade")
    class SearchActivityGuests {

        @Test
        @DisplayName("Retorna a lista paginada de convidados da atividade com sucesso")
        void searchActivityGuests_returnsCursorPage_whenActivityExistsAndEventIsPublished() {
            try (MockedStatic<CursorUtils> cursorUtilsMock = mockStatic(CursorUtils.class)) {
                EventActivity activity = mock(EventActivity.class);
                GuestCursor decodedCursor = mock(GuestCursor.class);
                ActivityGuest activityGuest = mock(ActivityGuest.class);
                CursorPage<ActivityGuest> expectedPage = mock(CursorPage.class);

                when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
                when(activity.getEvent()).thenReturn(event);
                when(event.isOwner(requesterId)).thenReturn(true);

                cursorUtilsMock.when(() -> CursorUtils.decode(any(), eq(GuestCursor.class))).thenReturn(decodedCursor);
                when(activityGuestDslRepository.findActivityGuestsWithCursor(activityId, true, decodedCursor, 20)).thenReturn(List.of(activityGuest));
                cursorUtilsMock.when(() -> CursorUtils.buildPage(anyList(), anyInt(), any())).thenReturn(expectedPage);

                CursorPage<ActivityGuest> result = guestService.searchActivityGuests(activityId, requesterId, "cursorRaw", 20);

                assertThat(result).isNotNull().isEqualTo(expectedPage);
                verify(activityGuestDslRepository).findActivityGuestsWithCursor(activityId, true, decodedCursor, 20);
            }
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando a atividade não é encontrada")
        void searchActivityGuests_throwsResourceNotFoundException_whenActivityNotFound() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> guestService.searchActivityGuests(activityId, requesterId, "cursorRaw", 20))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Atividade não encontrada.");

            verify(activityGuestDslRepository, never()).findActivityGuestsWithCursor(any(), anyBoolean(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("Update Event Guest - Atualização de Convidado")
    class UpdateEventGuest {

        @Test
        @DisplayName("Atualiza o status e a visibilidade do convidado quando o solicitante é o próprio convidado")
        void updateEventGuest_updatesSuccessfully_whenRequesterIsTheGuestHimself() {
            Long guestId = 5L;
            EventGuest guest = mock(EventGuest.class);
            UpdateGuestDTO dto = new UpdateGuestDTO(EnumGuestStatus.CANCELED, EnumGuestVisibility.PRIVATE);

            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            when(guest.getEvent()).thenReturn(event);
            when(event.isOwner(requesterId)).thenReturn(false);
            when(editorRepository.existsByEventIdAndUserId(event.getId(), requesterId)).thenReturn(false);
            when(guest.getUserId()).thenReturn(requesterId);

            guestService.updateEventGuest(eventId, guestId, requesterId, dto);

            verify(guest).setStatus(EnumGuestStatus.CANCELED);
            verify(guest).setVisibility(EnumGuestVisibility.PRIVATE);
            verify(eventGuestRepository).save(guest);
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o solicitante não é o gerente e nem o próprio convidado")
        void updateEventGuest_throwsAccessDeniedException_whenRequesterHasNoPermission() {
            Long guestId = 5L;
            EventGuest guest = mock(EventGuest.class);
            UpdateGuestDTO dto = new UpdateGuestDTO(EnumGuestStatus.CONFIRMED, EnumGuestVisibility.PUBLIC);

            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            when(guest.getEvent()).thenReturn(event);
            when(event.isOwner(requesterId)).thenReturn(false);
            when(editorRepository.existsByEventIdAndUserId(event.getId(), requesterId)).thenReturn(false);
            when(guest.getUserId()).thenReturn(UUID.randomUUID()); // Outro usuário

            assertThatThrownBy(() -> guestService.updateEventGuest(eventId, guestId, requesterId, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para alterar os dados deste convidado.");

            verify(eventGuestRepository, never()).save(any());
        }
    }
}