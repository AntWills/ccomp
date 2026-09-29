package com.ccomp.br.domain.events.activities.persistence.checkin;

import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CheckInCache {
    private final CheckInRepository checkInRepository;

    public CheckInCache(CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
    }

    @Cacheable(
            cacheNames = CacheNames.EVENT_ACTIVITY_CHECK_IN_BY_ACTIVITY_ID,
            key = "#activityId"
    )
    public Optional<CheckIn> findByActivityId(long activityId) {
        return checkInRepository.findByActivityId(activityId);
    }
}
