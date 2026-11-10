package com.ccomp.br.domain.events.enrollments.application;

import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentCapacityDTO;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentCapacityCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EnrollmentsServicesTest {
    private EnrollmentCapacityCache enrollmentCapacityCache;
    private EnrollmentsServices services;

    @BeforeEach
    void setUp() {
        enrollmentCapacityCache = mock(EnrollmentCapacityCache.class);
        services = new EnrollmentsServices(
                mock(EventRepository.class),
                mock(EventCache.class),
                mock(EnrollmentRepository.class),
                mock(EnrollmentDslRepository.class),
                mock(EventAccessPolicy.class),
                Clock.systemUTC(),
                enrollmentCapacityCache);
    }

    @Test
    void returnsCurrentEventEnrollmentCountAndMaximum() {
        when(enrollmentCapacityCache.getEventEnrollmentCapacity(42L))
                .thenReturn(new EnrollmentCapacityDTO(17, 80));

        EnrollmentCapacityDTO result = services.getEventEnrollmentCapacity(42L);

        assertThat(result).isEqualTo(new EnrollmentCapacityDTO(17, 80));
    }

    @Test
    void returnsCurrentActivityEnrollmentCountAndMaximum() {
        when(enrollmentCapacityCache.getActivityEnrollmentCapacity(9L))
                .thenReturn(new EnrollmentCapacityDTO(12, 25));

        EnrollmentCapacityDTO result = services.getActivityEnrollmentCapacity(9L);

        assertThat(result).isEqualTo(new EnrollmentCapacityDTO(12, 25));
    }

}
