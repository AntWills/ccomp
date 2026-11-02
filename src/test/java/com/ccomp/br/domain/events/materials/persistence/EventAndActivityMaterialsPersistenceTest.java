package com.ccomp.br.domain.events.materials.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterial;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialDslRepository;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import jakarta.persistence.EntityManager;
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
@Import({QueryDslConfig.class, EventMaterialDslRepository.class, ActivityMaterialDslRepository.class})
class EventAndActivityMaterialsPersistenceTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventMaterialRepository eventMaterials;

    @Autowired
    private EventMaterialDslRepository eventMaterialDsl;

    @Autowired
    private ActivityMaterialRepository activityMaterials;

    @Autowired
    private ActivityMaterialDslRepository activityMaterialDsl;

    @Test
    void savesAndQueriesLinksAndFilesPerOwningEntity() {
        Event event = Event.builder()
                .title("Evento materiais")
                .slug("evento-materiais-" + UUID.randomUUID())
                .category(EnumEventCategory.OTHER)
                .format(EnumEventFormat.ONLINE)
                .ownerId(UUID.randomUUID())
                .build();
        entityManager.persist(event);

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title("Atividade materiais")
                .type(EnumActivityType.OTHER)
                .displayOrder(0L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(activity);

        UUID creator = UUID.randomUUID();
        EventMaterial link = eventMaterials.saveAndFlush(EventMaterial.builder()
                .eventId(event.getId())
                .createdBy(creator)
                .title("Site")
                .type(EnumMaterialType.LINK)
                .url("https://example.org")
                .build());
        ActivityMaterial file = activityMaterials.saveAndFlush(ActivityMaterial.builder()
                .activityId(activity.getId())
                .createdBy(creator)
                .title("Imagem")
                .type(EnumMaterialType.IMAGE)
                .fileKey("activities/materials/a.png")
                .originalFileName("a.png")
                .contentType("image/png")
                .sizeBytes(12L)
                .build());
        entityManager.clear();

        assertThat(eventMaterialDsl.findAllByEventId(event.getId()))
                .extracting(EventMaterial::getId)
                .containsExactly(link.getId());
        assertThat(eventMaterials.findByIdAndEventId(link.getId(), event.getId())).isPresent();
        assertThat(eventMaterials.findByIdAndEventId(link.getId(), event.getId() + 999)).isEmpty();
        assertThat(activityMaterialDsl.findAllByActivityIdWithCursor(activity.getId(), null, 10))
                .extracting(ActivityMaterial::getId)
                .containsExactly(file.getId());
        assertThat(activityMaterials.findByIdAndActivityId(file.getId(), activity.getId())).isPresent();
    }
}
