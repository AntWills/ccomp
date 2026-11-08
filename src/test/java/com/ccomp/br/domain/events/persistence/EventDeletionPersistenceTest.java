package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterial;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import com.ccomp.br.domain.events.materials.persistence.EventMaterial;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EventDeletionPersistenceTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void deletingEventRemovesRowsThatOwnEveryEventStorageKey() {
        Event event = Event.builder()
                .title("Evento para exclusão")
                .slug("evento-exclusao-" + UUID.randomUUID())
                .category(EnumEventCategory.OTHER)
                .format(EnumEventFormat.ONLINE)
                .ownerId(UUID.randomUUID())
                .coverImageKey("events/cover.png")
                .build();
        entityManager.persist(event);
        entityManager.flush();

        EventActivity activity = EventActivity.builder()
                .event(event)
                .title("Atividade")
                .type(EnumActivityType.OTHER)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        entityManager.persist(activity);
        entityManager.flush();

        LocalDateTime createdAt = LocalDateTime.parse("2026-11-08T09:00:00");
        EventMaterial eventMaterial = EventMaterial.builder()
                .eventId(event.getId())
                .createdBy(event.getOwnerId())
                .createdAt(createdAt)
                .title("Material do evento")
                .type(EnumMaterialType.FILE)
                .fileKey("events/material.pdf")
                .originalFileName("material.pdf")
                .contentType("application/pdf")
                .sizeBytes(12L)
                .build();
        ActivityMaterial activityMaterial = ActivityMaterial.builder()
                .activityId(activity.getId())
                .createdBy(event.getOwnerId())
                .createdAt(createdAt)
                .title("Material da atividade")
                .type(EnumMaterialType.FILE)
                .fileKey("activities/material.pdf")
                .originalFileName("activity-material.pdf")
                .contentType("application/pdf")
                .sizeBytes(15L)
                .build();
        EventGuest guest = EventGuest.builder()
                .event(event)
                .name("Convidada")
                .imageKey("events/guest.png")
                .build();
        EventCoverTemplate coverTemplate = EventCoverTemplate.builder()
                .eventId(event.getId())
                .coverTemplateKey("events/certificate-cover.png")
                .build();

        entityManager.persist(eventMaterial);
        entityManager.persist(activityMaterial);
        entityManager.persist(guest);
        entityManager.persist(coverTemplate);
        entityManager.flush();

        Long eventId = event.getId();
        Long activityId = activity.getId();
        Long eventMaterialId = eventMaterial.getId();
        Long activityMaterialId = activityMaterial.getId();
        Long guestId = guest.getId();
        Long coverTemplateId = coverTemplate.getId();

        entityManager.clear();
        eventRepository.deleteById(eventId);
        eventRepository.flush();
        entityManager.clear();

        assertThat(eventRepository.findById(eventId)).isEmpty();
        assertThat(entityManager.find(EventMaterial.class, eventMaterialId)).isNull();
        assertThat(entityManager.find(ActivityMaterial.class, activityMaterialId)).isNull();
        assertThat(entityManager.find(EventActivity.class, activityId)).isNull();
        assertThat(entityManager.find(EventGuest.class, guestId)).isNull();
        assertThat(entityManager.find(EventCoverTemplate.class, coverTemplateId)).isNull();
    }
}
