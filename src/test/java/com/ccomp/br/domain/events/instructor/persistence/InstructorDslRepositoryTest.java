package com.ccomp.br.domain.events.instructor.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, InstructorDslRepository.class})
class InstructorDslRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private InstructorDslRepository instructorDslRepository;

    @Test
    void findByActivityReturnsOnlyConfirmedInstructorsIncludingAccountlessProfiles() {
        LocalDateTime now = LocalDateTime.now();
        Event event = Event.builder()
                .title("Evento dos ministrantes")
                .slug("evento-ministrantes-dsl")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);
        EventActivity activity = EventActivity.builder()
                .title("Atividade dos ministrantes")
                .type(EnumActivityType.LECTURE)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(activity);

        entityManager.persist(EventInstructor.builder()
                .activityId(activity.getId())
                .userId(UUID.randomUUID())
                .name("Professor")
                .build());
        entityManager.persist(EventInstructor.builder()
                .activityId(activity.getId())
                .userId(null)
                .name("Auxiliar sem conta")
                .build());
        entityManager.persist(EventInstructor.builder()
                .activityId(activity.getId())
                .userId(null)
                .name("Vínculo cancelado")
                .status(EnumInstructorStatus.CANCELED)
                .build());
        entityManager.flush();
        entityManager.clear();

        var results = instructorDslRepository.findByActivity(activity.getId());

        assertThat(results).hasSize(2)
                .allSatisfy(instructor -> assertThat(instructor.getStatus())
                        .isEqualTo(EnumInstructorStatus.CONFIRMED));
        assertThat(results).extracting(EventInstructor::getName)
                .containsExactlyInAnyOrder("Professor", "Auxiliar sem conta");
        assertThat(results).filteredOn(instructor -> instructor.getUserId() == null)
                .extracting(EventInstructor::getName)
                .containsExactly("Auxiliar sem conta");
    }
}
