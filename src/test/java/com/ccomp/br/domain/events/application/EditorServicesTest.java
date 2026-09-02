package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.external.dto.EditorAddedEvent;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.domain.events.persistence.editors.validation.EditorValidationCode;
import com.ccomp.br.domain.events.persistence.editors.validation.EditorValidationCodeRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
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
    private EditorValidationCodeRepository validationCodeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        @DisplayName("Adiciona o editor com sucesso quando o usuário não era editor anteriormente")
        void addEditor_returnsSuccessMessage_whenUserIsNotEditorYet() {
            UUID userId = UUID.randomUUID();

            EventEditor savedEditor = EventEditor.builder()
                    .id(10L)
                    .event(existingEvent)
                    .userId(userId)
                    .status(EnumEditorsStatus.PENDING)
                    .build();

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingEvent.getTitle()).thenReturn("Workshop de Java");
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(editorRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

            when(editorRepository.save(any(EventEditor.class))).thenReturn(savedEditor);

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response())
                    .isEqualTo("Usuário adicionado como editor com sucesso. Um e-mail de convite foi enviado.");

            verify(editorRepository).save(any(EventEditor.class));
            verify(validationCodeRepository).save(any(EditorValidationCode.class));
            verify(eventPublisher).publishEvent(any(EditorAddedEvent.class));
        }

        @Test
        @DisplayName("Retorna mensagem informando que usuário já é editor ativo quando registrado anteriormente")
        void addEditor_returnsAlreadyActiveMessage_whenUserIsAlreadyActiveEditor() {
            UUID userId = UUID.randomUUID();

            EventEditor activeEditor = mock(EventEditor.class);
            when(activeEditor.isActive()).thenReturn(true);

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(editorRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(activeEditor));

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Este usuário já é um editor ativo deste evento.");

            verify(editorRepository, never()).save(any());
            verify(validationCodeRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Reativa/reenvia convite quando usuário possui cadastro inativo ou revogado")
        void addEditor_reinvitesUser_whenEditorExistsButIsInactive() {
            UUID userId = UUID.randomUUID();

            EventEditor inactiveEditor = EventEditor.builder()
                    .id(5L)
                    .event(existingEvent)
                    .userId(userId)
                    .status(EnumEditorsStatus.REVOKED)
                    .build();

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingEvent.getTitle()).thenReturn("Workshop de Java");
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(editorRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(inactiveEditor));
            when(editorRepository.save(any(EventEditor.class))).thenReturn(inactiveEditor);

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response.response())
                    .isEqualTo("Usuário adicionado como editor com sucesso. Um e-mail de convite foi enviado.");

            verify(validationCodeRepository).deleteByEventEditor(inactiveEditor);
            verify(editorRepository).save(inactiveEditor);
            verify(validationCodeRepository).save(any(EditorValidationCode.class));
            verify(eventPublisher).publishEvent(any(EditorAddedEvent.class));
        }
    }

    @Nested
    @DisplayName("Aceitar Contive - Cenário de Exceção")
    class AcceptInvitation {
        @Test
        @DisplayName("Aceita o usuário quando o convite é valido")
        void acceptInvitation_acceptUser_whenCodeValid() {
            UUID code = UUID.randomUUID();

            EventEditor editor = mock(EventEditor.class);

            EditorValidationCode editorValidation = mock(EditorValidationCode.class);

            when(validationCodeRepository.findByCode(code)).thenReturn(Optional.of(editorValidation));
            when(editorValidation.isExpired()).thenReturn(false); // O código é valido
            when(editorValidation.getEventEditor()).thenReturn(editor);
            when(editor.isActive()).thenReturn(false); // Não é um editor ativo

            MessageResponse message = editorServices.acceptInvitation(code);

            assertThat(message).isNotNull();
            assertThat(message.response())
                    .isEqualTo("Convite aceito com sucesso. Você agora é um editor do evento.");

            verify(editorRepository).save(any(EventEditor.class));
            verify(validationCodeRepository).deleteByEventEditor(any(EventEditor.class));
        }

        @Test
        @DisplayName("Rejeita o usuário quando o convite é invalido")
        void acceptInvitation_throw_whenCodeInvalid() {
            UUID code = UUID.randomUUID();

            EventEditor editor = mock(EventEditor.class);
            EditorValidationCode editorValidation = mock(EditorValidationCode.class);

            when(validationCodeRepository.findByCode(code)).thenReturn(Optional.of(editorValidation));
            when(editorValidation.isExpired()).thenReturn(false); // O código é valido
            when(editor.isActive()).thenReturn(true);

            DomainException exception = assertThrows(DomainException.class, () ->
                editorServices.acceptInvitation(code)
            );

            assertThat(exception.getMessage())
                    .isEqualTo("O código de convite expirou.");

            verify(editorRepository, never()).save(any(EventEditor.class));
            verify(validationCodeRepository, never()).deleteByEventEditor(any(EventEditor.class));
        }

        @Test
        @DisplayName("Se o usuário já for editor, emite erro")
        void acceptInvitation_acceptUser_whenUserIsEditor() {
            UUID code = UUID.randomUUID();

            EventEditor editor = mock(EventEditor.class);
            EditorValidationCode editorValidation = mock(EditorValidationCode.class);

            when(validationCodeRepository.findByCode(code)).thenReturn(Optional.of(editorValidation));
            when(editorValidation.isExpired()).thenReturn(false); // O código é invalido
            when(editorValidation.getEventEditor()).thenReturn(editor);
            when(editor.isActive()).thenReturn(true); // É um editor ativo

            MessageResponse message = editorServices.acceptInvitation(code);

            assertThat(message).isNotNull();
            assertThat(message.response())
                    .isEqualTo("Você já é um editor ativo deste evento.");

            verify(editorRepository, never()).save(any(EventEditor.class));
            verify(validationCodeRepository).delete(any(EditorValidationCode.class));
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
