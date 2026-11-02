package com.ccomp.br.domain.events.materials.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.materials.dto.EventMaterialCursor;
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
@Import({QueryDslConfig.class, EventMaterialDslRepository.class})
class EventMaterialDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventMaterialDslRepository repository;

    @Test
    @DisplayName("Lista materiais por evento em ordem decrescente de criação e ID")
    void shouldListMaterialsByEventInDescendingOrder() {
        Event event = createEvent("evento-materiais-lista");
        LocalDateTime now = LocalDateTime.now();
        EventMaterial older = createLink(event, "Antigo", now.minusMinutes(1));
        EventMaterial earlierId = createLink(event, "Mesmo horário 1", now);
        EventMaterial laterId = createLink(event, "Mesmo horário 2", now);
        Event otherEvent = createEvent("outro-evento-materiais-lista");
        createLink(otherEvent, "Outro evento", now.plusMinutes(1));
        entityManager.flush();
        entityManager.clear();

        var materials = repository.findAllByEventId(event.getId());

        assertThat(materials).extracting(EventMaterial::getId)
                .containsExactly(laterId.getId(), earlierId.getId(), older.getId());
    }

    @Test
    @DisplayName("Continua a listagem de materiais usando cursor composto por data e ID")
    void shouldContinueListingMaterialsFromCursor() {
        Event event = createEvent("evento-materiais-cursor");
        LocalDateTime sameTime = LocalDateTime.now();
        EventMaterial first = createLink(event, "Primeiro", sameTime);
        EventMaterial second = createLink(event, "Segundo", sameTime);
        EventMaterial third = createLink(event, "Terceiro", sameTime);
        Event otherEvent = createEvent("outro-evento-materiais-cursor");
        createLink(otherEvent, "Fora da página", sameTime);
        entityManager.flush();
        entityManager.clear();

        var page1 = repository.findAllByEventIdWithCursor(event.getId(), null, 2);
        assertThat(page1).extracting(EventMaterial::getId)
                .containsExactly(third.getId(), second.getId());

        EventMaterial last = page1.getLast();
        var cursor = new EventMaterialCursor(last.getCreatedAt(), last.getId());
        var page2 = repository.findAllByEventIdWithCursor(event.getId(), cursor, 2);

        assertThat(page2).extracting(EventMaterial::getId).containsExactly(first.getId());
    }

    private Event createEvent(String slug) {
        Event event = Event.builder()
                .title("Evento de materiais")
                .slug(slug)
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(event);
        return event;
    }

    private EventMaterial createLink(Event event, String title, LocalDateTime createdAt) {
        EventMaterial material = EventMaterial.builder()
                .eventId(event.getId())
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
