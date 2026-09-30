package com.ccomp.br.domain.events.editors.persistence;

import com.ccomp.br.domain.events.editors.dto.EventEditorDTO;
import com.ccomp.br.domain.events.editors.utils.EventEditorMapper;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class EventEditorCache {
    private final EventEditorRepository editorRepository;
    private final EventEditorMapper eventEditorMapper;

    public EventEditorCache(EventEditorRepository editorRepository, EventEditorMapper eventEditorMapper) {
        this.editorRepository = editorRepository;
        this.eventEditorMapper = eventEditorMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EVENT_EDITOR_BY_EVENT_USER,
            key = "#eventId + ':' + #userId"
    )
    public Optional<EventEditorDTO> findByEventIdAndUserId(long eventId, UUID userId) {
        return editorRepository.findByEventIdAndUserId(eventId, userId)
                .map(eventEditorMapper::eventEditorToEventEditorDTO);
    }
}
