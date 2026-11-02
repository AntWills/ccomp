package com.ccomp.br.domain.events.activities.materials.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.materials.dto.MaterialCursor;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, ActivityMaterialDslRepository.class})
class ActivityMaterialDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActivityMaterialDslRepository repository;

    @Test
    @DisplayName("Continua a listagem de materiais da atividade usando cursor composto")
    void shouldContinueListingActivityMaterialsFromCursor() {
        Event event = createEvent();
        EventActivity activity = createActivity(event, "Atividade com materiais");
        EventActivity otherActivity = createActivity(event, "Outra atividade");
        LocalDateTime createdAt = LocalDateTime.now();
        ActivityMaterial first = createLink(activity, "Primeiro", createdAt);
        ActivityMaterial second = createLink(activity, "Segundo", createdAt);
        ActivityMaterial third = createLink(activity, "Terceiro", createdAt);
        createLink(otherActivity, "Material de outra atividade", createdAt);
        entityManager.flush();
        entityManager.clear();

        var page1 = repository.findAllByActivityIdWithCursor(activity.getId(), null, 2);
        assertThat(page1).extracting(ActivityMaterial::getId)
                .containsExactly(third.getId(), second.getId());

        ActivityMaterial last = page1.getLast();
        var cursor = new MaterialCursor(last.getCreatedAt(), last.getId());
        var page2 = repository.findAllByActivityIdWithCursor(activity.getId(), cursor, 2);

        assertThat(page2).extracting(ActivityMaterial::getId).containsExactly(first.getId());
    }

    private Event createEvent() {
        Event event = Event.builder()
                .title("Evento de materiais")
                .slug("evento-materiais-atividade-" + UUID.randomUUID())
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(event);
        return event;
    }

    private EventActivity createActivity(Event event, String title) {
        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(title)
                .type(EnumActivityType.OTHER)
                .displayOrder(0L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(activity);
        return activity;
    }

    private ActivityMaterial createLink(EventActivity activity, String title, LocalDateTime createdAt) {
        ActivityMaterial material = ActivityMaterial.builder()
                .activityId(activity.getId())
                .createdBy(UUID.randomUUID())
                .title(title)
                .type(EnumMaterialType.LINK)
                .url("https://example.org/" + title.toLowerCase().replace(' ', '-'))
                .createdAt(createdAt)
                .build();
        entityManager.persist(material);
        return material;
    }
}
