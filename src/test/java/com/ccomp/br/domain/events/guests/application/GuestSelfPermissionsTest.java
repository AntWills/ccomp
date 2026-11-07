package com.ccomp.br.domain.events.guests.application;

import java.util.UUID;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestSelfPermissionsTest {
    @Mock private EventGuestRepository eventGuestRepository;
    @Mock private EventCache eventCache;
    @Mock private ActivityCache activityCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventGuestDslRepository eventGuestDslRepository;
    @Mock private ActivityGuestDslRepository activityGuestDslRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventActivityRepository activityRepository;
    @Mock private ActivityGuestRepository activityGuestRepository;
    @Mock private UserManagement userManagement;
    @InjectMocks private GuestService service;

    @Test
    void linkedGuestCanEditActivityWhenAccountIsActiveAndProfileIsAssociated() {
        long activityId = 13L;
        UUID userId = UUID.randomUUID();
        when(userManagement.isAccountActive(userId)).thenReturn(true);
        when(activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, userId)).thenReturn(true);

        assertThat(service.canEditActivity(activityId, userId)).isTrue();
    }

    @Test
    void linkedGuestCannotEditActivityWhenAccountIsInactive() {
        long activityId = 13L;
        UUID userId = UUID.randomUUID();
        when(userManagement.isAccountActive(userId)).thenReturn(false);

        assertThat(service.canEditActivity(activityId, userId)).isFalse();
    }

    @Test
    void linkedGuestCannotEditActivityWithoutAssociation() {
        long activityId = 13L;
        UUID userId = UUID.randomUUID();
        when(userManagement.isAccountActive(userId)).thenReturn(true);
        when(activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, userId)).thenReturn(false);

        assertThat(service.canEditActivity(activityId, userId)).isFalse();
    }
}
