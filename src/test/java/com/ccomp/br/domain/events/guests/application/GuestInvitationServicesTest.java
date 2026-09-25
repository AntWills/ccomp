package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.events.guests.dto.EventInvitationCursor;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitation;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitationDslRepository;
import com.ccomp.br.domain.events.guests.persistence.invitations.EventInvitationRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
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
class GuestInvitationServicesTest {

    @Mock
    private EventGuestRepository eventGuestRepository;

    @Mock
    private EventInvitationRepository eventInvitationRepository;

    @Mock
    private EventInvitationDslRepository eventInvitationDspRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventEditorRepository editorRepository;

    @Mock
    private UserManagement userManagement;

    @InjectMocks
    private GuestInvitationServices guestInvitationServices;

    private UUID userId;
    private Long eventId;
    private Event event;
    private EmailAddress emailAddress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = 1L;
        event = mock(Event.class);
        emailAddress = mock(EmailAddress.class);
    }

    @Nested
    @DisplayName("Search Invitations - Busca Paginada de Convites")
    class SearchInvitations {

        @Test
        @DisplayName("Deve buscar convites com sucesso quando o usuário tem permissão para gerenciar o evento")
        void searchInvitations_returnsCursorPage_whenUserHasPermission() {
            try (MockedStatic<CursorUtils> cursorUtilsMock = mockStatic(CursorUtils.class)) {
                EventInvitationCursor decodedCursor = mock(EventInvitationCursor.class);
                EventInvitation invitation = mock(EventInvitation.class);
                CursorPage<EventInvitation> expectedPage = mock(CursorPage.class);

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(event.isOwner(userId)).thenReturn(true);
                cursorUtilsMock.when(() -> CursorUtils.decode(any(), eq(EventInvitationCursor.class))).thenReturn(decodedCursor);
                when(eventInvitationDspRepository.findAllWithCursor(emailAddress, eventId, decodedCursor, 20)).thenReturn(List.of(invitation));
                cursorUtilsMock.when(() -> CursorUtils.buildPage(anyList(), anyInt(), any())).thenReturn(expectedPage);

                CursorPage<EventInvitation> result = guestInvitationServices.searchInvitations(userId, emailAddress, eventId, "cursorRaw", 20);

                assertThat(result).isNotNull().isEqualTo(expectedPage);
                verify(eventInvitationDspRepository).findAllWithCursor(emailAddress, eventId, decodedCursor, 20);
            }
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não possui permissão de gerenciar o evento")
        void searchInvitations_throwsAccessDeniedException_whenUserHasNoPermission() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(event.isOwner(userId)).thenReturn(false);
            when(editorRepository.existsByEventIdAndUserId(event.getId(), userId)).thenReturn(false);

            assertThatThrownBy(() -> guestInvitationServices.searchInvitations(userId, emailAddress, eventId, "cursorRaw", 20))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Você não tem permissão para gerenciar/enviar convites neste evento.");

            verify(eventInvitationDspRepository, never()).findAllWithCursor(any(), any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("Invite - Envio de Convite")
    class Invite {

        @Test
        @DisplayName("Envia convite com sucesso para usuário ativo cadastrado na plataforma")
        void invite_sendsInvitationSuccessfully_whenUserIsActive() {
            UserDTO userDTO = mock(UserDTO.class);
            UUID guestUserId = UUID.randomUUID();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(event.isOwner(userId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(userDTO));
            when(userDTO.isActive()).thenReturn(true);
            when(userDTO.id()).thenReturn(guestUserId);
            when(eventGuestRepository.existsByUserIdAndEvent(guestUserId, event)).thenReturn(false);
            when(eventInvitationRepository.findByEmailAddressAndEvent(emailAddress, event)).thenReturn(Optional.empty());

            MessageResponse response = guestInvitationServices.invite(userId, eventId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Um e-mail de convite foi enviado.");
            verify(eventInvitationRepository).save(any(EventInvitation.class));
        }

        @Test
        @DisplayName("Lança UserBlockedException quando o usuário encontrado está inativo ou bloqueado")
        void invite_throwsUserBlockedException_whenUserIsInactive() {
            UserDTO userDTO = mock(UserDTO.class);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(event.isOwner(userId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(userDTO));
            when(userDTO.isActive()).thenReturn(false);

            assertThatThrownBy(() -> guestInvitationServices.invite(userId, eventId, emailAddress))
                    .isInstanceOf(UserBlockedException.class)
                    .hasMessage("O usuário informado está inativo ou bloqueado.");

            verify(eventInvitationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Accept Invite - Aceite ou Recusa de Convite")
    class AcceptInvite {

        @Test
        @DisplayName("Aceita o convite com sucesso e registra o convidado no evento")
        void acceptInvite_acceptsInvitationSuccessfully() {
            UUID code = UUID.randomUUID();
            EventInvitation invitation = mock(EventInvitation.class);
            UserDTO userDTO = mock(UserDTO.class);

            when(eventInvitationRepository.findByCode(code)).thenReturn(Optional.of(invitation));
            when(invitation.isValid()).thenReturn(true);
            when(userManagement.findById(userId)).thenReturn(Optional.of(userDTO));
            when(userDTO.emailAddress()).thenReturn(emailAddress);
            when(invitation.isSameEmail(emailAddress)).thenReturn(true);
            when(invitation.getEvent()).thenReturn(event);

            MessageResponse response = guestInvitationServices.acceptInvite(userId, code, true);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Convite aceito com sucesso.");
            verify(invitation).accept();
            verify(eventInvitationRepository).save(invitation);
            verify(eventGuestRepository).save(any());
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o e-mail do usuário não coincide com o do convite")
        void acceptInvite_throwsAccessDeniedException_whenEmailDoesNotMatch() {
            UUID code = UUID.randomUUID();
            EventInvitation invitation = mock(EventInvitation.class);
            UserDTO userDTO = mock(UserDTO.class);

            when(eventInvitationRepository.findByCode(code)).thenReturn(Optional.of(invitation));
            when(invitation.isValid()).thenReturn(true);
            when(userManagement.findById(userId)).thenReturn(Optional.of(userDTO));
            when(userDTO.emailAddress()).thenReturn(emailAddress);
            when(invitation.isSameEmail(emailAddress)).thenReturn(false);

            assertThatThrownBy(() -> guestInvitationServices.acceptInvite(userId, code, true))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Usuário não autorizado.");

            verify(invitation, never()).accept();
            verify(eventGuestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cancel Invitation - Cancelamento de Convite")
    class CancelInvitation {

        @Test
        @DisplayName("Cancela o convite enviado com sucesso quando o usuário tem permissão")
        void cancelInvitation_cancelsInvitationSuccessfully_whenUserHasPermission() {
            Long invitationId = 10L;
            EventInvitation invitation = mock(EventInvitation.class);

            when(eventInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(invitation.getEvent()).thenReturn(event);
            when(event.isOwner(userId)).thenReturn(true);

            MessageResponse response = guestInvitationServices.cancelInvitation(userId, invitationId);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Convite cancelado com sucesso.");
            verify(invitation).cancel();
            verify(eventInvitationRepository).save(invitation);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando o convite não é encontrado")
        void cancelInvitation_throwsResourceNotFoundException_whenInvitationNotFound() {
            Long invitationId = 10L;
            when(eventInvitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> guestInvitationServices.cancelInvitation(userId, invitationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Convite não encontrado.");

            verify(eventInvitationRepository, never()).save(any());
        }
    }
}