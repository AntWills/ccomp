package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ActivityCache {
    private final EventActivityRepository activityRepository;
    private final EventActivityDslRepository activityDslRepository;
    private final ActivityMapper activityMapper;

    public ActivityCache(EventActivityRepository activityRepository,
                         EventActivityDslRepository activityDslRepository,
                         ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.activityDslRepository = activityDslRepository;
        this.activityMapper = activityMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.EVENT_ACTIVITY_BY_ID, key = "#activityId")
    public Optional<ActivityDTO> findById(Long activityId) {
        return activityRepository.findById(activityId).map(activityMapper::eventActivityToActivityDTO);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.EVENT_ACTIVITIES,
            key = "#eventId + ':' + (#cursor == null ? 'first' : #cursor.displayOrder() + '-' + #cursor.id()) + ':' + #limit")
    public List<EventActivityDTO> findAllByEventIdWithCursor(Long eventId, EventActivityCursor cursor, int limit) {
        return activityDslRepository.findAllByEventIdWithCursor(eventId, cursor, limit);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.EVENT_ACTIVITY_BY_ID, key = "#activity.id"),
            @CacheEvict(cacheNames = CacheNames.EVENT_ACTIVITIES, allEntries = true)
    })
    public void evict(EventActivity activity) {
    }

    @CacheEvict(cacheNames = CacheNames.EVENT_ACTIVITIES, allEntries = true)
    public void evictEventLists() {
    }
}
