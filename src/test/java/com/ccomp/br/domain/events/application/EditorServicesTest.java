package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
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
    @DisplayName("Add editor - Cenários de Exceção")
    class AddEditor {
        @Test
        @DisplayName("Adicione o editor caso o evento exista")
        void addEditor_returnMessage_whenEventExist() {
            UUID userId = UUID.randomUUID();

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(existingUserDTO.isTeamMember()).thenReturn(true);
            when(editorRepository.existsByEventIdAndUserId(existingEvent.getId(), existingUserDTO.id()))
                    .thenReturn(false); // Para adicionar, o usuario não pode ser editor antes.

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Usuário adicionado como editor com sucesso.");

            verify(editorRepository).save(any(EventEditor.class));
        }

        @Test
        @DisplayName("Retorna mensagem informando que usuário já é editor quando registrado anteriormente")
        void addEditor_returnMessageIsEditor_whenEventExist() {
            UUID userId = UUID.randomUUID();

            when(existingEvent.getId()).thenReturn(eventId);
            when(existingUserDTO.id()).thenReturn(userId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.isOwner(ownerId)).thenReturn(true);
            when(userManagement.findByEmailAddress(emailAddress)).thenReturn(Optional.of(existingUserDTO));
            when(existingUserDTO.isActive()).thenReturn(true);
            when(existingUserDTO.isTeamMember()).thenReturn(true);
            when(editorRepository.existsByEventIdAndUserId(existingEvent.getId(), existingUserDTO.id()))
                    .thenReturn(true); // Para emitir a mensagem, o usuario deve ser editor antes.

            MessageResponse response = editorServices.addEditor(eventId, ownerId, emailAddress);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Este usuário já é um editor deste evento.");

            verify(editorRepository, never()).save(any());
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
}
