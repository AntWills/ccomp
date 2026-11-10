package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentCapacityDTO;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EnrollmentCapacityCacheTest {
    private EventRepository eventRepository;
    private EventActivityRepository activityRepository;
    private EnrollmentCapacityCache cache;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        activityRepository = mock(EventActivityRepository.class);
        cache = new EnrollmentCapacityCache(eventRepository, activityRepository);
    }

    @Test
    void returnsEventActiveCountAndCapacity() {
        when(eventRepository.findById(42L)).thenReturn(Optional.of(
                Event.builder().activeEnrollmentCount(17).maxCapacity(80).build()));

        assertThat(cache.getEventEnrollmentCapacity(42L)).isEqualTo(new EnrollmentCapacityDTO(17, 80));
    }

    @Test
    void returnsZeroAndCapacityWhenActivityHasNoActiveEnrollments() {
        when(activityRepository.findById(9L)).thenReturn(Optional.of(
                EventActivity.builder().maxCapacity(25).activeEnrollmentCount(0).build()));

        assertThat(cache.getActivityEnrollmentCapacity(9L)).isEqualTo(new EnrollmentCapacityDTO(0, 25));
    }

    @Test
    void reportsMissingEventOrActivity() {
        when(eventRepository.findById(404L)).thenReturn(Optional.empty());
        when(activityRepository.findById(405L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cache.getEventEnrollmentCapacity(404L));
        assertThrows(ResourceNotFoundException.class, () -> cache.getActivityEnrollmentCapacity(405L));
    }
}
