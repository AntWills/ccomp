package com.ccomp.br.domain.events.activities.persistence.checkin;

import com.ccomp.br.domain.events.activities.dto.CheckInDTO;
import com.ccomp.br.domain.events.activities.utils.CheckInMapper;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CheckInCache {
    private final CheckInRepository checkInRepository;
    private final CheckInMapper checkInMapper;

    public CheckInCache(CheckInRepository checkInRepository, CheckInMapper checkInMapper) {
        this.checkInRepository = checkInRepository;
        this.checkInMapper = checkInMapper;
    }

    @Cacheable(
            cacheNames = CacheNames.EVENT_ACTIVITY_CHECK_IN_BY_ACTIVITY_ID,
            key = "#activityId"
    )
    public Optional<CheckInDTO> findByActivityId(long activityId) {
        return checkInRepository.findByActivityId(activityId).map(checkInMapper::checkInToCheckInDTO);
    }
}
