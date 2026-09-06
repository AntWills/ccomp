package com.ccomp.br.domain.events.application;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.events.external.dto.EditorAddedMessageDTO;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.persistence.editors.validation.EventEditorInvitations;
import com.ccomp.br.domain.events.persistence.editors.validation.EventEditorInvitationsRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EditorServicesTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventEditorRepository editorRepository;
    @Mock
    private UserManagement userManagement;
    @Mock
    private EventEditorInvitationsRepository invitationsRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    EditorServices editorServices;

    private UUID ownerId;
    private Long eventId;
    private Event existingEvent;
    private UserDTO existingUserDTO;
    private EmailAddress emailAddress;

    @BeforeEach
    void setUp () {
        ownerId = UUID.randomUUID();
        eventId = 1L;
        existingEvent = mock(Event.class);
        emailAddress = mock(EmailAddress.class);
        existingUserDTO = mock(UserDTO.class);
    }
    @Nested
    @DisplayName("Add Editor - Cenários Principais")
    class AddEditor {

        @Test
        @DisplayName("Convidadar enviado com sucesso, caso o usuário já não seja editor")
        void addEditor_returnsSuccessMessage_whenUserIsNotEditorYet() {
            UUID userId = UUID.randomUUID();

            // Dados
            when(existingUserDTO.id()).thenReturn(userId);
            when(existingEvent.getId()).thenReturn(eventId);

            // Mock getEventAndValidateOwnership() privado
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);

            // Mock usuario e editor
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(editorRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);

            // Moack reissueInvitation() privado
            when(invitationsRepository.findByEmailAddressAndEventId(emailAddress, eventId))
                    .thenReturn(Optional.empty());

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response())
                    .isEqualTo("Um e-mail de convite foi enviado.");

            verify(invitationsRepository).save(any(EventEditorInvitations.class));
            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE_NAME),
                    eq(RabbitMQConfig.ROUTING_KEY_EDITOR_INVITATION),
                    any(EditorAddedMessageDTO.class)
            );
        }

        @Test
        @DisplayName("Retorna mensagem informando que usuário já é editor ativo quando registrado anteriormente")
        void addEditor_returnsAlreadyActiveMessage_whenUserIsAlreadyActiveEditor() {
            UUID userId = UUID.randomUUID();

            // Dados
            when(existingUserDTO.id()).thenReturn(userId);

            // Mock getEventAndValidateOwnership() privado
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);

            // Mock usuario e editor
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(editorRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(true); // Mudança

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response)
                    .isNotNull();
            assertThat(response.response())
                    .isEqualTo("Este usuário já é um editor ativo deste evento.");

            verify(invitationsRepository, never()).save(any(EventEditorInvitations.class));
            verify(rabbitTemplate, never()).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE_NAME),
                    eq(RabbitMQConfig.ROUTING_KEY_EDITOR_INVITATION),
                    any(EditorAddedMessageDTO.class)
            );
        }
    }

    @Nested
    @DisplayName("Aceitar Contive - Cenário de Exceção")
    class AcceptInvitation {
        @Test
        @DisplayName("Aceita o usuário quando o convite é valido")
        void acceptInvitation_acceptUser_whenCodeValid() {
            // Dados
            UUID code = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            EventEditorInvitations invitations = mock(EventEditorInvitations.class);

            when(invitations.getEmailAddress()).thenReturn(emailAddress);
            when(invitations.isExpired()).thenReturn(false); // O código é valido
            when(invitations.getEventId()).thenReturn(eventId);
            when(existingUserDTO.isActive()).thenReturn(true);
            when(existingUserDTO.id()).thenReturn(userId);
            when(existingEvent.getTitle()).thenReturn("Nome evento");

            // Fluxo lógico
            when(invitationsRepository.findByCode(code)).thenReturn(Optional.of(invitations));
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(editorRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            MessageResponse message = editorServices.acceptInvitation(code, userId);

            assertThat(message).isNotNull();
            assertThat(message.response())
                    .isEqualTo("Convite aceito com sucesso. Você agora é um editor do evento %s.".formatted("Nome evento"));

            verify(editorRepository).save(any(EventEditor.class));
            verify(invitationsRepository).delete(any(EventEditorInvitations.class));
        }

        @Test
        @DisplayName("Rejeita o usuário já é editor")
        void acceptInvitation_throw_whenCodeInvalid() {
            // Dados
            UUID code = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            EventEditorInvitations invitations = mock(EventEditorInvitations.class);

            when(invitations.getEmailAddress()).thenReturn(emailAddress);
            when(invitations.isExpired()).thenReturn(false); // O código é valido
            when(invitations.getEventId()).thenReturn(eventId);
            when(existingUserDTO.isActive()).thenReturn(true);
            when(existingUserDTO.id()).thenReturn(userId);

            // Fluxo lógico
            when(invitationsRepository.findByCode(code)).thenReturn(Optional.of(invitations));
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(editorRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

            MessageResponse message = editorServices.acceptInvitation(code, userId);

            assertThat(message).isNotNull();
            assertThat(message.response())
                    .isEqualTo("Você já é um editor ativo deste evento.");

            verify(editorRepository, never()).save(any(EventEditor.class));
            verify(invitationsRepository).delete(any(EventEditorInvitations.class));
        }
    }

    @Nested
    @DisplayName("Remover Editor - Cenários de Exceção")
    class RemoveEditor {
        @Test
        @DisplayName("Remover o editor caso o ele exista")
        void removeEditor_returnMessage_whenEditorExist() {
            UUID userId = UUID.randomUUID();

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(existingEvent.getId())).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(editorRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

            MessageResponse message = editorServices.removeEditor(eventId, ownerId, emailAddress);

            assertThat(message).isNotNull();
            assertThat(message.response()).isEqualTo("Usuário removido como editor.");

            verify(editorRepository).deleteByEventIdAndUserId(eventId, userId);
        }
    }

    @Nested
    @DisplayName("Has Permission Edit - Verificação de Permissão")
    class HasPermissionEdit {

        @Test
        @DisplayName("Retorna true quando o usuário é um editor ativo do evento")
        void hasPermissionEdit_returnsTrue_whenUserIsActiveEditor() {
            Event event = mock(Event.class);
            UUID userId = UUID.randomUUID();
            Long eventId = 1L;

            EventEditor activeEditor = mock(EventEditor.class);
            when(activeEditor.isActive()).thenReturn(true);

            when(event.getId()).thenReturn(eventId);
            when(editorRepository.findByEventIdAndUserId(eventId, userId))
                    .thenReturn(Optional.of(activeEditor));

            boolean hasPermission = editorServices.hasPermissionEdit(event, userId);

            assertThat(hasPermission).isTrue();
            verify(editorRepository).findByEventIdAndUserId(eventId, userId);
        }

        @Test
        @DisplayName("Retorna false quando o usuário não é um editor ou o cadastro não está ativo")
        void hasPermissionEdit_returnsFalse_whenEditorNotFoundOrInactive() {
            Event event = mock(Event.class);
            UUID userId = UUID.randomUUID();
            Long eventId = 1L;

            when(event.getId()).thenReturn(eventId);

            // Cenário 1: Editor não encontrado no banco de dados
            when(editorRepository.findByEventIdAndUserId(eventId, userId))
                    .thenReturn(Optional.empty());

            boolean hasPermissionNotFound = editorServices.hasPermissionEdit(event, userId);

            assertThat(hasPermissionNotFound).isFalse();

            // Cenário 2: Editor encontrado, mas inativo/pendente/revogado
            EventEditor inactiveEditor = mock(EventEditor.class);
            when(inactiveEditor.isActive()).thenReturn(false);

            when(editorRepository.findByEventIdAndUserId(eventId, userId))
                    .thenReturn(Optional.of(inactiveEditor));

            boolean hasPermissionInactive = editorServices.hasPermissionEdit(event, userId);

            assertThat(hasPermissionInactive).isFalse();
        }
    }
}
