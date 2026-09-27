package com.ccomp.br.domain.events.editors.application;

import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class EventEditorPermission {
    private final EventEditorRepository editorRepository;

    public EventEditorPermission(EventEditorRepository editorRepository) {
        this.editorRepository = editorRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EVENT_EDITOR_HAS_PERMISSION,
            key = "#eventId + ':' + #userId"
    )
    public boolean hasPermissionEdit(Long eventId, UUID userId) {
        return editorRepository
                .findByEventIdAndUserId(eventId, userId)
                .map(EventEditor::isActive)
                .orElse(false);
    }

    @CacheEvict(cacheNames = CacheNames.EVENT_EDITOR_HAS_PERMISSION, key = "#eventId + ':' + #userId")
    public void hasPermissionEditEvict(Long eventId, UUID userId) {
    }
}
