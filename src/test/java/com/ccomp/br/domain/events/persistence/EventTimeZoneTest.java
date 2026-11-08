package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTimeZoneTest {

    @Test
    void publishingSameDayEventUsesItsFortalezaLocalTimeEvenWithUtcClock() {
        Event event = event("America/Fortaleza", LocalDateTime.of(2026, 11, 8, 9, 5),
                LocalDateTime.of(2026, 11, 8, 12, 0));

        event.publish(utcClock("2026-11-08T12:10:00Z")); // 09:10 em Fortaleza

        assertThat(event.getExecutionStatus(utcClock("2026-11-08T12:10:00Z")))
                .isEqualTo(EnumEventExecutionStatus.IN_PROGRESS);
    }

    @Test
    void publishingFinishedEventIsRejectedAtLocalTime() {
        Event event = event("America/Fortaleza", LocalDateTime.of(2026, 11, 8, 9, 5),
                LocalDateTime.of(2026, 11, 8, 12, 0));

        assertThatThrownBy(() -> event.publish(utcClock("2026-11-08T15:01:00Z"))) // 12:01 em Fortaleza
                .isInstanceOf(DomainException.class)
                .hasMessage("Não é possível publicar um evento que já foi finalizado.");
    }

    @Test
    void sameInstantIsComparedInEachEventsOwnZone() {
        Clock instant = utcClock("2026-11-08T15:10:00Z");
        Event fortaleza = event("America/Fortaleza", LocalDateTime.of(2026, 11, 8, 9, 0),
                LocalDateTime.of(2026, 11, 8, 12, 0));
        Event saoPaulo = event("America/Sao_Paulo", LocalDateTime.of(2026, 11, 8, 12, 0),
                LocalDateTime.of(2026, 11, 8, 18, 0));

        assertThat(fortaleza.getExecutionStatus(instant)).isEqualTo(EnumEventExecutionStatus.FINISHED);
        assertThat(saoPaulo.getExecutionStatus(instant)).isEqualTo(EnumEventExecutionStatus.IN_PROGRESS);
    }

    @Test
    void eventRejectsInvalidIanaTimeZone() {
        Event event = event("America/Fortaleza", LocalDateTime.of(2026, 11, 8, 9, 0),
                LocalDateTime.of(2026, 11, 8, 12, 0));

        assertThatThrownBy(() -> event.setTimeZone("Fortaleza"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("identificador IANA válido");
    }

    @Test
    void enrollmentWindowUsesTheEventsLocalTime() {
        Event event = event("America/Fortaleza", LocalDateTime.of(2026, 11, 8, 9, 5),
                LocalDateTime.of(2026, 11, 8, 12, 0));
        event.setStatus(EnumEventStatus.PUBLISHED);
        event.setEnrollmentStartDate(LocalDateTime.of(2026, 11, 8, 9, 0));
        event.setEnrollmentEndDate(LocalDateTime.of(2026, 11, 8, 12, 0));

        assertThat(event.getEnrollmentStatus(utcClock("2026-11-08T12:10:00Z")))
                .isEqualTo(EnumEnrollmentStatus.OPEN);
        assertThat(event.getEnrollmentStatus(utcClock("2026-11-08T15:00:00Z")))
                .isEqualTo(EnumEnrollmentStatus.CLOSED);
    }

    private static Event event(String zone, LocalDateTime start, LocalDateTime end) {
        Event event = Event.builder().startDate(start).endDate(end).build();
        event.setTimeZone(zone);
        return event;
    }

    private static Clock utcClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
