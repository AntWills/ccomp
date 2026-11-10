package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentCapacityDTO;
import com.ccomp.br.shared.cache.CacheNames;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EnrollmentCapacityCache {
    private final EventRepository eventRepository;
    private final EventActivityRepository activityRepository;
    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;

    public EnrollmentCapacityCache(EventRepository eventRepository,
                                   EventActivityRepository activityRepository,
                                   EnrollmentActivityDslRepository enrollmentActivityDslRepository) {
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.EVENT_ENROLLMENT_CAPACITY, key = "#eventId")
    public EnrollmentCapacityDTO getEventEnrollmentCapacity(Long eventId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        return new EnrollmentCapacityDTO(event.getActiveEnrollmentCount(), event.getMaxCapacity());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.ACTIVITY_ENROLLMENT_CAPACITY, key = "#activityId")
    public EnrollmentCapacityDTO getActivityEnrollmentCapacity(Long activityId) {
        var activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        // countActiveByActivityId returns zero when no active enrollment rows exist.
        long currentEnrollments = enrollmentActivityDslRepository.countActiveByActivityId(activityId);
        return new EnrollmentCapacityDTO(currentEnrollments, activity.getMaxCapacity());
    }
}
