package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.editors.application.EditorServices;
import com.ccomp.br.domain.events.editors.application.EventEditorPermission;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventAccessPolicy {
    private final EventEditorPermission editorPermission;

    public EventAccessPolicy(EventEditorPermission editorPermission) {
        this.editorPermission = editorPermission;
    }

    public void assertCanView(Event event, @Nullable UUID userId) {
        boolean allowed = event.isPubliclyAccessible()
                || (userId != null && event.isOwner(userId))
                || (userId != null && editorPermission.hasPermissionEdit(event.getId(), userId))
                || SecurityUtils.isModeratorOrAdmin();

        if(allowed)
            return;

        throw denied();
    }

    public void assertCanEdit(Event event, UUID userId) {
        boolean allowed = (userId != null && event.isOwner(userId))
                || (userId != null && editorPermission.hasPermissionEdit(event.getId(), userId))
                || SecurityUtils.isModeratorOrAdmin();

        if(allowed)
            return;

        throw denied();
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }
}
