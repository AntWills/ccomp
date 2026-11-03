package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.dto.CreateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestDTO;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {
    @Mock private EventGuestRepository eventGuestRepository;
    @Mock private EventCache eventCache;
    @Mock private ActivityCache activityCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventGuestDslRepository eventGuestDslRepository;
    @Mock private ActivityGuestDslRepository activityGuestDslRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventActivityRepository activityRepository;
    @Mock private ActivityGuestRepository activityGuestRepository;
    @Mock private UserManagement userManagement;

    @InjectMocks private GuestService guestService;

    private Long eventId;
    private Long activityId;
    private UUID requesterId;
    private EventDTO event;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        activityId = 10L;
        requesterId = UUID.randomUUID();
        event = mock(EventDTO.class);
    }

    private void givenEventExists() {
        when(eventCache.findById(eventId)).thenReturn(Optional.of(event));
    }

    @Nested
    class SearchEventGuests {
        @Test
        @DisplayName("Deve retornar a página de convidados públicos quando o evento pode ser visualizado")
        void returnsCursorPageForPublicEvent() {
            try (MockedStatic<CursorUtils> cursorUtils = mockStatic(CursorUtils.class)) {
                givenEventExists();
                GuestCursor cursor = mock(GuestCursor.class);
                EventGuestDTO guest = mock(EventGuestDTO.class);
                CursorPage<EventGuestDTO> expected = mock(CursorPage.class);
                when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(false);
                when(eventAccessPolicy.canView(event, requesterId)).thenReturn(true);
                when(eventGuestDslRepository.findGuestsWithCursor(eventId, false, cursor, 20)).thenReturn(List.of(guest));
                cursorUtils.when(() -> CursorUtils.decode("raw", GuestCursor.class)).thenReturn(cursor);
                cursorUtils.when(() -> CursorUtils.buildPage(anyList(), eq(20), any())).thenReturn(expected);

                assertThat(guestService.searchEventGuests(eventId, requesterId, "raw", 20)).isSameAs(expected);
                verify(eventGuestDslRepository).findGuestsWithCursor(eventId, false, cursor, 20);
            }
        }

        @Test
        @DisplayName("Deve limitar a página a 50 convidados e permitir ao gerente visualizar convidados privados")
        void capsPageSizeAtFiftyAndAllowsManagersToSeePrivateGuests() {
            try (MockedStatic<CursorUtils> cursorUtils = mockStatic(CursorUtils.class)) {
                givenEventExists();
                GuestCursor cursor = mock(GuestCursor.class);
                when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);
                when(eventAccessPolicy.canView(event, requesterId)).thenReturn(true);
                when(eventGuestDslRepository.findGuestsWithCursor(eventId, true, cursor, 50)).thenReturn(List.of());
                cursorUtils.when(() -> CursorUtils.decode(null, GuestCursor.class)).thenReturn(cursor);
                cursorUtils.when(() -> CursorUtils.buildPage(anyList(), eq(50), any())).thenReturn(mock(CursorPage.class));

                guestService.searchEventGuests(eventId, requesterId, null, 100);
                verify(eventGuestDslRepository).findGuestsWithCursor(eventId, true, cursor, 50);
            }
        }

        @Test
        @DisplayName("Deve negar a consulta quando o evento não pode ser visualizado pelo solicitante")
        void deniesPrivateEventToNonManager() {
            givenEventExists();
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(false);
            when(eventAccessPolicy.canView(event, requesterId)).thenReturn(false);
            assertThatThrownBy(() -> guestService.searchEventGuests(eventId, requesterId, null, 20))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(eventGuestDslRepository);
        }

        @Test
        @DisplayName("Deve lançar erro quando o evento não existe")
        void throwsWhenEventDoesNotExist() {
            when(eventCache.findById(eventId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> guestService.searchEventGuests(eventId, requesterId, null, 20))
                    .isInstanceOf(ResourceNotFoundException.class).hasMessage("Evento não encontrado.");
        }
    }

    @Nested
    class SearchActivityGuests {
        @Test
        @DisplayName("Deve retornar convidados quando o evento da atividade pode ser visualizado")
        void returnsGuestsWhenActivityEventIsViewable() {
            try (MockedStatic<CursorUtils> cursorUtils = mockStatic(CursorUtils.class)) {
                ActivityDTO activity = mock(ActivityDTO.class);
                GuestCursor cursor = mock(GuestCursor.class);
                ActivityGuestDTO guest = mock(ActivityGuestDTO.class);
                CursorPage<ActivityGuestDTO> expected = mock(CursorPage.class);

                when(activityCache.findById(activityId)).thenReturn(Optional.of(activity));
                when(activity.eventId()).thenReturn(eventId);
                givenEventExists();
                when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);
                when(eventAccessPolicy.canView(event, requesterId)).thenReturn(true);
                when(activityGuestDslRepository.findActivityGuestsWithCursor(
                        activityId,
                        true,
                        cursor,
                        20)
                ).thenReturn(List.of(guest));

                cursorUtils.when(() -> CursorUtils.decode("raw", GuestCursor.class)).thenReturn(cursor);
                cursorUtils.when(() -> CursorUtils.buildPage(anyList(), eq(20), any())).thenReturn(expected);

                assertThat(guestService.searchActivityGuests(activityId, requesterId, "raw", 20)).isSameAs(expected);
            }
        }

        @Test
        @DisplayName("Deve lançar erro quando a atividade não existe")
        void throwsWhenActivityDoesNotExist() {
            when(activityCache.findById(activityId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> guestService.searchActivityGuests(activityId, requesterId, null, 20))
                    .isInstanceOf(ResourceNotFoundException.class).hasMessage("Atividade não encontrada.");
        }

        @Test
        @DisplayName("Deve negar a consulta quando o evento da atividade não pode ser visualizado")
        void deniesWhenActivityEventIsNotViewable() {
            ActivityDTO activity = mock(ActivityDTO.class);
            when(activityCache.findById(activityId)).thenReturn(Optional.of(activity));
            when(activity.eventId()).thenReturn(eventId);
            givenEventExists();
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(false);
            when(eventAccessPolicy.canView(event, requesterId)).thenReturn(false);

            assertThatThrownBy(() -> guestService.searchActivityGuests(activityId, requesterId, null, 20))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(activityGuestDslRepository);
        }
    }

    @Nested
    class UpdateEventGuest {
        @Test
        @DisplayName("Deve permitir ao convidado atualizar o próprio status e visibilidade")
        void allowsGuestToUpdateTheirOwnStatusAndVisibility() {
            Long guestId = 5L;
            EventGuest guest = mock(EventGuest.class);
            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            when(guest.getEventId()).thenReturn(eventId);
            givenEventExists();
            when(guest.getUserId()).thenReturn(requesterId);
            UpdateGuestDTO dto = new UpdateGuestDTO(EnumGuestStatus.CANCELED, EnumGuestVisibility.PRIVATE);

            guestService.updateEventGuest(eventId, guestId, requesterId, dto);

            verify(guest).setStatus(EnumGuestStatus.CANCELED);
            verify(guest).setVisibility(EnumGuestVisibility.PRIVATE);
            verify(eventGuestRepository).save(guest);
        }

        @Test
        @DisplayName("Deve permitir ao gerente atualizar outro convidado")
        void allowsManagerToUpdateAnotherGuest() {
            Long guestId = 5L;
            EventGuest guest = mock(EventGuest.class);
            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            when(guest.getEventId()).thenReturn(eventId);
            givenEventExists();
            when(guest.getUserId()).thenReturn(UUID.randomUUID());
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);

            guestService.updateEventGuest(eventId, guestId, requesterId,
                    new UpdateGuestDTO(EnumGuestStatus.CONFIRMED, null));

            verify(guest).setStatus(EnumGuestStatus.CONFIRMED);
            verify(guest, never()).setVisibility(any());
            verify(eventGuestRepository).save(guest);
        }

        @Test
        @DisplayName("Deve negar a atualização solicitada por usuário sem permissão")
        void deniesUpdateByUnrelatedUser() {
            Long guestId = 5L;
            EventGuest guest = mock(EventGuest.class);
            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            when(guest.getEventId()).thenReturn(eventId);
            givenEventExists();
            when(guest.getUserId()).thenReturn(UUID.randomUUID());
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(false);

            assertThatThrownBy(() -> guestService.updateEventGuest(eventId, guestId, requesterId,
                    new UpdateGuestDTO(EnumGuestStatus.CONFIRMED, EnumGuestVisibility.PUBLIC)))
                    .isInstanceOf(AccessDeniedException.class);
            verify(eventGuestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro quando o convidado não existe no evento")
        void throwsWhenGuestDoesNotExist() {
            when(eventGuestRepository.findByIdAndEventId(5L, eventId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> guestService.updateEventGuest(eventId, 5L, requesterId,
                    new UpdateGuestDTO(null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Convidado não encontrado neste evento.");
        }
    }

    @Nested
    class ManageGuestProfiles {
        @Test
        @DisplayName("Cria convidado sem conta e o associa à atividade informada")
        void createsAccountlessGuestAndAssociatesActivity() {
            Event persistedEvent = mock(Event.class);
            EventActivity activity = mock(EventActivity.class);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(persistedEvent));
            givenEventExists();
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
            when(activity.getEventId()).thenReturn(eventId);
            when(eventGuestRepository.save(any(EventGuest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            EventGuest result = guestService.createGuest(
                    eventId,
                    requesterId,
                    new CreateGuestDTO("Ada Lovelace", "Palestrante", activityId)
            );

            assertThat(result.getName()).isEqualTo("Ada Lovelace");
            assertThat(result.getDescription()).isEqualTo("Palestrante");
            assertThat(result.getUserId()).isNull();
            verify(activityGuestRepository).save(argThat(link ->
                    link.getActivity() == activity && link.getEventGuest() == result
            ));
        }

        @Test
        @DisplayName("Atualiza nome e descrição somente para quem pode editar o evento")
        void updatesGuestProfileForEventEditor() {
            EventGuest guest = mock(EventGuest.class);
            when(eventGuestRepository.findByIdAndEventId(5L, eventId)).thenReturn(Optional.of(guest));
            givenEventExists();
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);

            guestService.updateGuestProfile(
                    eventId,
                    5L,
                    requesterId,
                    new UpdateGuestProfileDTO("Grace Hopper", "Convidada especial")
            );

            verify(guest).setName("Grace Hopper");
            verify(guest).setDescription("Convidada especial");
            verify(eventGuestRepository).save(guest);
        }

        @Test
        @DisplayName("Vincula a conta encontrada por e-mail à atividade do convidado")
        void linksAccountByEmailToActivity() {
            Long guestId = 5L;
            String email = "ada@example.com";
            UUID guestUserId = UUID.randomUUID();
            EventGuest guest = mock(EventGuest.class);
            EventActivity activity = mock(EventActivity.class);
            UserDTO user = new UserDTO(
                    guestUserId,
                    "Ada Lovelace",
                    new EmailAddress(email),
                    EnumUserStatusAccount.ACTIVE,
                    null,
                    null,
                    EnumRoles.USER,
                    null
            );

            when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
            givenEventExists();
            when(eventAccessPolicy.canEdit(event, requesterId)).thenReturn(true);
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
            when(activity.getEventId()).thenReturn(eventId);
            when(userManagement.findByEmailAddress(new EmailAddress(email))).thenReturn(Optional.of(user));
            when(activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, guestUserId))
                    .thenReturn(false);

            guestService.linkAccount(eventId, guestId, requesterId, email, activityId);

            verify(guest).setUserId(guestUserId);
            verify(eventGuestRepository).save(guest);
            verify(activityGuestRepository).save(any(ActivityGuest.class));
        }
    }
}
