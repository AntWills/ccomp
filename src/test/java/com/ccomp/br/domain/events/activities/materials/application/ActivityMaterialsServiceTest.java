package com.ccomp.br.domain.events.activities.materials.application;

import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialDslRepository;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.materials.dto.CreateMaterialLinkRequest;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import com.ccomp.br.domain.events.materials.utils.MaterialMapper;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityMaterialsServiceTest {
    private static final Long ACTIVITY_ID = 30L;
    private static final Long EVENT_ID = 20L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private EventActivityRepository activities;

    @Mock
    private EventRepository events;

    @Mock
    private ActivityMaterialRepository materials;

    @Mock
    private ActivityMaterialDslRepository materialDsl;

    @Mock
    private EventAccessPolicy access;

    @Mock
    private StorageExternal storage;

    @Mock
    private StorageKeyGenerator keyGenerator;

    private ActivityMaterialsService service;
    private EventActivity activity;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new ActivityMaterialsService(
                activities, events, materials, access, materialDsl, storage, keyGenerator, new MaterialMapper());
        activity = mock(EventActivity.class);
        event = Event.builder().id(EVENT_ID).build();
        when(activities.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(EVENT_ID);
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    @Test
    void activityLinkUsesParentEventAuthorization() {
        when(materials.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.addLink(
                ACTIVITY_ID,
                USER_ID,
                new CreateMaterialLinkRequest("Slides", "", "http://example.org/slides"));

        assertThat(response.type()).isEqualTo(EnumMaterialType.LINK);
        assertThat(response.url()).isEqualTo("http://example.org/slides");
        verify(access).assertCanEdit(event, USER_ID);
    }

    @Test
    void listChecksParentEventVisibility() {
        service.list(ACTIVITY_ID, null, null, 10);

        verify(access).assertCanView(event, null);
        verify(materialDsl).findAllByActivityIdWithCursor(ACTIVITY_ID, null, 11);
    }
}
