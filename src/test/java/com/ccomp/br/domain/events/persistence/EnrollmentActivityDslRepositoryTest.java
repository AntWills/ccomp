package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;
import com.ccomp.br.domain.events.persistence.activities.*;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, EnrollmentActivityDslRepository.class})
public class EnrollmentActivityDslRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventActivityRepository activityRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentActivityRepository enrollmentActivityRepository;

    @Autowired
    private EnrollmentActivityDslRepository dslRepository;

    private UUID userId;
    private Event event;
    private EventActivity activity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // 1. Persistir Evento pai
        event = eventRepository.saveAndFlush(
                Event.builder()
                        .title("Workshop de Java")
                        .slug("workshop-java-" + UUID.randomUUID().toString().substring(0, 5))
                        .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                        .format(EnumEventFormat.IN_PERSON)
                        .status(EnumEventStatus.PUBLISHED)
                        .ownerId(UUID.randomUUID())
                        .startDate(LocalDateTime.now().plusDays(1))
                        .endDate(LocalDateTime.now().plusDays(1).plusHours(4))
                        .build()
        );

        // 2. Persistir Atividade do Evento
        activity = activityRepository.saveAndFlush(
                EventActivity.builder()
                        .event(event)
                        .title("Palestra Inicial")
                        .type(EnumActivityType.LECTURE)
                        .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    @Test
    @DisplayName("Deve retornar a relação de inscrição com atividade quando userId e activityId existirem")
    void shouldFindEnrollmentActivityByUserIdAndActivityId() {
        // Setup: Persistir a inscrição no evento e a vinculação com a atividade
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.builder()
                        .event(event)
                        .userId(userId)
                        .status(EnumEnrollmentState.CONFIRMED)
                        .build()
        );

        EnrollmentActivity enrollmentActivity = enrollmentActivityRepository.saveAndFlush(
                EnrollmentActivity.builder()
                        .enrollment(enrollment)
                        .activity(activity)
                        .build()
        );

        // Action
        Optional<EnrollmentActivity> result = dslRepository.findByUserIdAndActivityId(userId, activity.getId());

        // Assertions
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(enrollmentActivity.getId());
        assertThat(result.get().getEnrollment().getUserId()).isEqualTo(userId);
        assertThat(result.get().getActivity().getId()).isEqualTo(activity.getId());
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando o userId informado não possuir inscrição na atividade")
    void shouldReturnEmptyWhenUserIdDoesNotExist() {
        // Setup: Inscrição criada para um usuário X
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.builder()
                        .event(event)
                        .userId(userId)
                        .status(EnumEnrollmentState.CONFIRMED)
                        .build()
        );

        enrollmentActivityRepository.saveAndFlush(
                EnrollmentActivity.builder()
                        .enrollment(enrollment)
                        .activity(activity)
                        .build()
        );

        // Action: Busca realizada com um userId diferente
        UUID anotherUserId = UUID.randomUUID();
        Optional<EnrollmentActivity> result = dslRepository.findByUserIdAndActivityId(anotherUserId, activity.getId());

        // Assertions
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando o activityId informado for inexistente")
    void shouldReturnEmptyWhenActivityIdDoesNotExist() {
        // Setup: Inscrição criada para a atividade atual
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.builder()
                        .event(event)
                        .userId(userId)
                        .status(EnumEnrollmentState.CONFIRMED)
                        .build()
        );

        enrollmentActivityRepository.saveAndFlush(
                EnrollmentActivity.builder()
                        .enrollment(enrollment)
                        .activity(activity)
                        .build()
        );

        // Action: Busca com um ID de atividade que não existe
        Long nonexistentActivityId = 999L;
        Optional<EnrollmentActivity> result = dslRepository.findByUserIdAndActivityId(userId, nonexistentActivityId);

        // Assertions
        assertThat(result).isEmpty();
    }
}