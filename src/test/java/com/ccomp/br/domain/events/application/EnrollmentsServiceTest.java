package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.enums.EnumEnrollmentStatus;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentBlaze;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.domain.events.util.EnrollmentMapper;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrollmentsServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentBlaze enrollmentBlaze;

    @Mock
    private UserManagement userManagement;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @InjectMocks
    private EnrollmentsServices enrollmentsServices;

    private Long eventId;
    private Event existingEvent;
    private Enrollment existingEnrollment;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        existingEvent = mock(Event.class);
        existingEnrollment = mock(Enrollment.class);
    }

    @Nested
    @DisplayName("Subscribe - Cenários de Exceção")
    class Subscribe {
        @Test
        @DisplayName("Inscreve qualquer usuário a um evento exista")
        void subscribe_returnSuccess_whenNotSubscriber() {
            UUID userId = UUID.randomUUID();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.getEnrollmentStatus()).thenReturn(EnumEnrollmentStatus.OPEN);
            when(enrollmentRepository.findByUserIdAndEvent(userId, existingEvent)).thenReturn(Optional.empty());

            MessageResponse response = enrollmentsServices.subscribe(userId, eventId);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Inscrição realizada com sucesso.");

            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Reativa a inscrição de quem já foi inscrito um evento exista")
        void subscribe_returnSuccess_whenAlreadyBeenSubscriber() {
            UUID userId = UUID.randomUUID();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.getEnrollmentStatus()).thenReturn(EnumEnrollmentStatus.OPEN);
            when(enrollmentRepository.findByUserIdAndEvent(userId, existingEvent)).thenReturn(Optional.of(existingEnrollment));
            when(existingEnrollment.isActive()).thenReturn(false);

            MessageResponse response = enrollmentsServices.subscribe(userId, eventId);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Inscrição realizada com sucesso.");

            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Tenta se inscrever, mesmo já estando inscrito, em um evento que exista")
        void subscribe_returnSuccess_whenAlreadyBeenActive() {
            UUID userId = UUID.randomUUID();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(existingEvent.getEnrollmentStatus()).thenReturn(EnumEnrollmentStatus.OPEN);
            when(enrollmentRepository.findByUserIdAndEvent(userId, existingEvent)).thenReturn(Optional.of(existingEnrollment));
            when(existingEnrollment.isActive()).thenReturn(true); // Indica que u usuário tem inscrição ativa.

            MessageResponse response = enrollmentsServices.subscribe(userId, eventId);

            assertThat(response).isNotNull();
            assertThat(response.response()).isEqualTo("Inscrição realizada com sucesso.");

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }
    }
}
