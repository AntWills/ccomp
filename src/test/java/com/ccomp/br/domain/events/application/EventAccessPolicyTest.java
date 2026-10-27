package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.editors.dto.EventEditorDTO;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.editors.persistence.EventEditorCache;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAccessPolicyTest {
    private static final Long EVENT_ID = 14L;
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID EDITOR_ID = UUID.randomUUID();

    @Mock private EventEditorCache editorCache;

    private EventAccessPolicy policy;
    private EventDTO privateEvent;
    private Event privateModel;

    @BeforeEach
    void setUp() {
        policy = new EventAccessPolicy(editorCache);
        privateEvent = eventDto(EnumEventStatus.DRAFT, OWNER_ID);
        privateModel = Event.builder().id(EVENT_ID).ownerId(OWNER_ID).status(EnumEventStatus.DRAFT).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicEventCanBeViewedWithoutConsultingEditorCache() {
        assertThat(policy.canView(eventDto(EnumEventStatus.PUBLISHED, OWNER_ID), null)).isTrue();
        policy.assertCanView(eventModel(EnumEventStatus.PUBLISHED, OWNER_ID), null);

        verifyNoInteractions(editorCache);
    }

    @Test
    void ownerCanViewAndEditPrivateEvent() {
        assertThat(policy.canView(privateEvent, OWNER_ID)).isTrue();
        assertThat(policy.canEdit(privateEvent, OWNER_ID)).isTrue();
        policy.assertCanView(privateModel, OWNER_ID);
        policy.assertCanEdit(privateModel, OWNER_ID);

        verifyNoInteractions(editorCache);
    }

    @Test
    void activeEditorCanViewAndEditPrivateEvent() {
        when(editorCache.findByEventIdAndUserId(EVENT_ID, EDITOR_ID))
                .thenReturn(Optional.of(editor(EnumEditorsStatus.ACTIVE)));

        assertThat(policy.canView(privateEvent, EDITOR_ID)).isTrue();
        assertThat(policy.canEdit(privateEvent, EDITOR_ID)).isTrue();
        policy.assertCanView(privateModel, EDITOR_ID);
        policy.assertCanEdit(privateModel, EDITOR_ID);
    }

    @Test
    void inactiveOrMissingEditorCannotViewOrEditPrivateEvent() {
        when(editorCache.findByEventIdAndUserId(EVENT_ID, EDITOR_ID))
                .thenReturn(Optional.of(editor(EnumEditorsStatus.REVOKED)), Optional.empty());

        assertThat(policy.canView(privateEvent, EDITOR_ID)).isFalse();
        assertThat(policy.canEdit(privateEvent, EDITOR_ID)).isFalse();
        assertThat(policy.canEdit(privateEvent, null)).isFalse();
        assertThatThrownBy(() -> policy.assertCanView(privateModel, EDITOR_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Você não tem permissão para acessar este arquivo.");
        assertThatThrownBy(() -> policy.assertCanEdit(privateModel, EDITOR_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Você não tem permissão para acessar este arquivo.");
    }

    @Test
    void moderatorAndAdminCanEditPrivateEventsAndModeratorCanViewPrivateEvent() {
        authenticateAs("ROLE_MODERATOR");
        assertThat(policy.canEdit(privateEvent, null)).isTrue();
        policy.assertCanEdit(privateModel, null);

        authenticateAs("ADMIN");
        assertThat(policy.canEdit(privateEvent, EDITOR_ID)).isTrue();
        policy.assertCanView(privateModel, EDITOR_ID);

        verify(editorCache).findByEventIdAndUserId(EVENT_ID, EDITOR_ID);
    }

    @Test
    void authenticatedNonModeratorDoesNotBypassOwnerOrEditorRules() {
        authenticateAs("ROLE_USER");

        assertThat(policy.canEdit(privateEvent, EDITOR_ID)).isFalse();
        assertThatThrownBy(() -> policy.assertCanEdit(privateModel, EDITOR_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activeEditorLookupUsesEventAndUserIds() {
        when(editorCache.findByEventIdAndUserId(EVENT_ID, EDITOR_ID))
                .thenReturn(Optional.of(editor(EnumEditorsStatus.ACTIVE)));

        assertThat(policy.isActiveEditor(EVENT_ID, EDITOR_ID)).isTrue();
        verify(editorCache).findByEventIdAndUserId(EVENT_ID, EDITOR_ID);
    }

    private EventDTO eventDto(EnumEventStatus status, UUID ownerId) {
        return new EventDTO(EVENT_ID, "Evento", "evento", null, null, null,
                null, null, null, status,
                null, null, null, null, null, false, null,
                ownerId, null, null, null);
    }

    private Event eventModel(EnumEventStatus status, UUID ownerId) {
        return Event.builder().id(EVENT_ID).ownerId(ownerId).status(status).build();
    }

    private EventEditorDTO editor(EnumEditorsStatus status) {
        return new EventEditorDTO(1L, EVENT_ID, EDITOR_ID, null, null, status);
    }

    private void authenticateAs(String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "test-user", "", List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
