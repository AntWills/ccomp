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
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventClockTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 8, 9, 5);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 8, 12, 0);
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Test
    void publishesAt0910BrasiliaWhenEventEndsAtNoon() {
        Event event = event();

        event.publish(now("2026-11-08T12:10:00Z")); // 09:10 em Brasília

        assertThat(event.getExecutionStatus(now("2026-11-08T12:10:00Z")))
                .isEqualTo(EnumEventExecutionStatus.IN_PROGRESS);
        assertThat(event.getStatus()).isEqualTo(EnumEventStatus.PUBLISHED);
    }

    @Test
    void publishingAtEndTimeIsRejectedBecauseEndBoundaryIsExclusive() {
        Event event = event();

        // O comportamento atual considera finalizado no próprio instante de end_date.
        assertThatThrownBy(() -> event.publish(now("2026-11-08T15:00:00Z"))) // 12:00 em Brasília
                .isInstanceOf(DomainException.class)
                .hasMessage("Não é possível publicar um evento que já foi finalizado.");
    }

    @Test
    void publishingAt1201BrasiliaIsRejected() {
        Event event = event();

        assertThatThrownBy(() -> event.publish(now("2026-11-08T15:01:00Z"))) // 12:01 em Brasília
                .isInstanceOf(DomainException.class)
                .hasMessage("Não é possível publicar um evento que já foi finalizado.");
    }

    @Test
    void publishingBeforeStartIsAllowedAndEventIsNotStarted() {
        Event event = event();
        LocalDateTime now = now("2026-11-08T11:00:00Z"); // 08:00 em Brasília

        event.publish(now);

        assertThat(event.getExecutionStatus(now)).isEqualTo(EnumEventExecutionStatus.NOT_STARTED);
        assertThat(event.getStatus()).isEqualTo(EnumEventStatus.PUBLISHED);
    }

    @Test
    void sameDayEventIsValid() {
        Event event = event();

        event.setStartDate(START);
        event.setEndDate(END);

        assertThat(event.getStartDate()).isEqualTo(START);
        assertThat(event.getEndDate()).isEqualTo(END);
    }

    @Test
    void enrollmentWindowUsesTheInjectedBrasiliaClockAndClosesAtEnd() {
        Event event = event();
        event.setStatus(EnumEventStatus.PUBLISHED);
        event.setEnrollmentStartDate(LocalDateTime.of(2026, 11, 8, 9, 0));
        event.setEnrollmentEndDate(END);

        assertThat(event.getEnrollmentStatus(now("2026-11-08T12:10:00Z")))
                .isEqualTo(EnumEnrollmentStatus.OPEN);
        assertThat(event.getEnrollmentStatus(now("2026-11-08T15:00:00Z")))
                .isEqualTo(EnumEnrollmentStatus.CLOSED);
    }

    private static Event event() {
        return Event.builder().startDate(START).endDate(END).build();
    }

    private static LocalDateTime now(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), BRASILIA);
        return LocalDateTime.now(clock);
    }
}
