package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.editors.dto.EventEditorDTO;
import com.ccomp.br.domain.events.editors.persistence.EventEditorCache;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventAccessPolicy {
    private final EventEditorCache editorCache;

    public EventAccessPolicy(EventEditorCache editorCache) {
        this.editorCache = editorCache;
    }

    public boolean canView(EventDTO event, @Nullable UUID userId) {
        return event.isPubliclyAccessible()
                || canEdit(event, userId);
    }

    public boolean canEdit(EventDTO event, @Nullable UUID userId) {
        if (SecurityUtils.isModeratorOrAdmin()) return true;
        if (userId == null) return false;
        return event.isOwner(userId) || isActiveEditor(event.id(), userId);
    }

    public boolean isActiveEditor(Long eventId, UUID userId) {
        return editorCache
                .findByEventIdAndUserId(eventId, userId)
                .map(EventEditorDTO::isActive)
                .orElse(false);
    }

    public void assertCanView(Event event, @Nullable UUID userId) {
        boolean allowed = event.isPubliclyAccessible()
                || (userId != null && event.isOwner(userId))
                || (userId != null && isActiveEditor(event.getId(), userId))
                || SecurityUtils.isModeratorOrAdmin();

        if(allowed)
            return;

        throw denied();
    }

    public void assertCanEdit(Event event, UUID userId) {
        boolean allowed = (userId != null && event.isOwner(userId))
                || (userId != null && isActiveEditor(event.getId(), userId))
                || SecurityUtils.isModeratorOrAdmin();

        if(allowed)
            return;

        throw denied();
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }
}
