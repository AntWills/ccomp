package com.ccomp.br.domain.events.enrollments.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentSearchResult;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentCapacityCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.shared.utils.CursorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentsSearchTest {
    @Mock private EventRepository eventRepository;
    @Mock private EventCache eventCache;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EnrollmentDslRepository enrollmentDslRepository;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private Clock clock;
    @Mock private EnrollmentCapacityCache enrollmentCapacityCache;
    @Mock private EnrollmentActivityRepository enrollmentActivityRepository;
    @Mock private EventActivityRepository activityRepository;
    @InjectMocks private EnrollmentsServices service;

    @Test
    void combinesSearchAndStatusAndReturnsRelevanceCursorForNextPage() {
        UUID manager = UUID.randomUUID();
        EventDTO event = mock(EventDTO.class);
        when(eventCache.findById(8L)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canEdit(event, manager)).thenReturn(true);
        LocalDateTime firstTime = LocalDateTime.parse("2026-11-01T10:00:00");
        LocalDateTime secondTime = firstTime.minusHours(1);
        var first = result("Ana Silva", 10L, firstTime, 0.98);
        var second = result("Ana Silv", 9L, secondTime, 0.72);
        when(enrollmentDslRepository.searchWithCursor(8L, EnumEnrollmentState.CONFIRMED,
                "Ana Silva", null, 2)).thenReturn(List.of(first, second));

        var page = service.searchEnrollments(8L, manager, null, 1,
                EnumEnrollmentState.CONFIRMED, " Ana Silva ");

        assertThat(page.content()).containsExactly(first.enrollment());
        assertThat(page.nextCursor()).isNotBlank();
        assertThat(CursorUtils.decode(page.nextCursor(), EnrollmentsCursor.class))
                .isEqualTo(new EnrollmentsCursor(0.98, firstTime, 10L));
        verify(enrollmentDslRepository).searchWithCursor(8L, EnumEnrollmentState.CONFIRMED,
                "Ana Silva", null, 2);
    }

    @Test
    void decodesRelevanceCursorAndForwardsItWithTheSameFilters() {
        UUID manager = UUID.randomUUID();
        EventDTO event = mock(EventDTO.class);
        when(eventCache.findById(8L)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canEdit(event, manager)).thenReturn(true);
        EnrollmentsCursor cursor = new EnrollmentsCursor(0.72,
                LocalDateTime.parse("2026-11-01T09:00:00"), 9L);
        String encoded = CursorUtils.encode(cursor);
        when(enrollmentDslRepository.searchWithCursor(8L, EnumEnrollmentState.CONFIRMED,
                "Ana Silva", cursor, 11)).thenReturn(List.of());

        var page = service.searchEnrollments(8L, manager, encoded, 10,
                EnumEnrollmentState.CONFIRMED, "Ana Silva");

        assertThat(page.content()).isEmpty();
        verify(enrollmentDslRepository).searchWithCursor(8L, EnumEnrollmentState.CONFIRMED,
                "Ana Silva", cursor, 11);
    }

    private EnrollmentSearchResult result(String name, Long id, LocalDateTime createdAt, double relevance) {
        var user = new UserSummaryDTO(UUID.randomUUID(), name,
                new EmailAddress(name.toLowerCase().replace(' ', '.') + "@test.com"));
        var enrollment = new EnrollmentListItem(user, id, EnumEnrollmentState.CONFIRMED, createdAt);
        return new EnrollmentSearchResult(enrollment, relevance);
    }
}
