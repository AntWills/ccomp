package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.ResolvedActivityCertificateTemplate;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomaticCertificateServicesTest {
    private static final long EVENT_ID = 12L;
    private static final long ACTIVITY_ID = 34L;
    private static final long RECIPIENT_ID = 56L;
    private static final long GUEST_ID = 78L;
    private static final UUID RECIPIENT = UUID.randomUUID();
    private static final UUID REQUESTER = UUID.randomUUID();

    @Mock private EventCache eventCache;
    @Mock private EventAccessPolicy accessPolicy;
    @Mock private CertificateFetcher certificateFetcher;
    @Mock private CertificateVariablesServices variablesServices;
    @Mock private ActivityCache activityCache;
    @Mock private ActivityGuestRepository activityGuestRepository;

    private AutomaticCertificateServices services;
    private StorageExternal.StoredFile cover;
    private EventDTO event;

    @BeforeEach
    void setUp() throws Exception {
        services = new AutomaticCertificateServices(eventCache, accessPolicy, certificateFetcher,
                variablesServices, activityCache, activityGuestRepository);
        cover = createCover();
        event = mock(EventDTO.class);
    }

    @Test
    void buildCertificateFromActivityGeneratesCertificate() {
        ActivityDTO activity = activity();
        ResolvedActivityCertificateTemplate template = activityTemplate(EnumActivityCertificateTemplateType.PARTICIPANT);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(certificateFetcher.resolveActivityTemplate(EVENT_ID, ACTIVITY_ID,
                EnumActivityCertificateTemplateType.PARTICIPANT)).thenReturn(template);
        when(variablesServices.loadActivityVariablesValues(List.of(), ACTIVITY_ID, RECIPIENT)).thenReturn(Map.of());

        assertThat(services.buildCertificateFromActivity(ACTIVITY_ID, RECIPIENT).contentType()).isEqualTo("image/png");
    }

    @Test
    void buildCertificateFromActivityFailsWhenActivityIsMissing() {
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> services.buildCertificateFromActivity(ACTIVITY_ID, RECIPIENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buildCertificateFromInstructorGeneratesCertificateForRecipient() {
        when(variablesServices.findInstructorCertificateRecipient(EVENT_ID, RECIPIENT_ID)).thenReturn(RECIPIENT);
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID, EnumEventCertificateTemplateType.INSTRUCTOR))
                .thenReturn(boxTemplate());
        when(variablesServices.loadInstructorVariablesValues(List.of(), EVENT_ID, RECIPIENT_ID, RECIPIENT))
                .thenReturn(Map.of());

        assertThat(services.buildCertificateFromInstructor(EVENT_ID, RECIPIENT_ID, RECIPIENT).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void buildCertificateFromInstructorRejectsAnotherUserWithoutEditPermission() {
        when(variablesServices.findInstructorCertificateRecipient(EVENT_ID, RECIPIENT_ID)).thenReturn(RECIPIENT);
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, REQUESTER)).thenReturn(false);

        assertThatThrownBy(() -> services.buildCertificateFromInstructor(EVENT_ID, RECIPIENT_ID, REQUESTER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void buildCertificateFromActivityGuestGeneratesCertificateForConfirmedPresenter() {
        ActivityDTO activity = activity();
        EventGuest guest = mock(EventGuest.class);
        ActivityGuest link = mock(ActivityGuest.class);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activityGuestRepository.findByActivityIdAndId(ACTIVITY_ID, GUEST_ID)).thenReturn(Optional.of(link));
        when(link.getRole()).thenReturn(EnumActivityGuestRole.PRESENTER);
        when(link.getEventGuest()).thenReturn(guest);
        when(guest.isConfirmed()).thenReturn(true);
        when(guest.getUserId()).thenReturn(RECIPIENT);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID, EnumEventCertificateTemplateType.GUEST)).thenReturn(boxTemplate());
        when(variablesServices.loadGuestInstructorVariablesValues(EVENT_ID, RECIPIENT)).thenReturn(Map.of());

        assertThat(services.buildCertificateFromActivityGuest(EVENT_ID, ACTIVITY_ID, GUEST_ID, RECIPIENT).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void buildCertificateFromActivityGuestFailsWhenActivityIsMissing() {
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> services.buildCertificateFromActivityGuest(EVENT_ID, ACTIVITY_ID, GUEST_ID, RECIPIENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buildActivityCertificateFromInstructorGeneratesCertificate() {
        ActivityDTO activity = activity();
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(variablesServices.findActivityInstructorCertificateRecipient(EVENT_ID, ACTIVITY_ID, RECIPIENT_ID))
                .thenReturn(RECIPIENT);
        when(certificateFetcher.resolveActivityTemplate(EVENT_ID, ACTIVITY_ID,
                EnumActivityCertificateTemplateType.INSTRUCTOR))
                .thenReturn(activityTemplate(EnumActivityCertificateTemplateType.INSTRUCTOR));
        when(variablesServices.loadActivityRoleVariables(List.of(), ACTIVITY_ID, RECIPIENT, EnumCertificateType.INSTRUCTOR))
                .thenReturn(Map.of());

        assertThat(services.buildActivityCertificateFromInstructor(EVENT_ID, ACTIVITY_ID, RECIPIENT_ID, RECIPIENT)
                .contentType()).isEqualTo("image/png");
    }

    @Test
    void buildActivityCertificateFromInstructorFailsWhenActivityIsMissing() {
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> services.buildActivityCertificateFromInstructor(
                EVENT_ID, ACTIVITY_ID, RECIPIENT_ID, RECIPIENT)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buildActivityCertificateFromGuestGeneratesCertificate() {
        ActivityDTO activity = activity();
        ActivityGuest link = mock(ActivityGuest.class);
        EventGuest guest = mock(EventGuest.class);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activityGuestRepository.findByActivityIdAndId(ACTIVITY_ID, GUEST_ID)).thenReturn(Optional.of(link));
        when(link.getEventGuest()).thenReturn(guest);
        when(link.getRole()).thenReturn(EnumActivityGuestRole.PRESENTER);
        when(guest.isConfirmed()).thenReturn(true);
        when(guest.getUserId()).thenReturn(RECIPIENT);
        when(certificateFetcher.resolveActivityTemplate(EVENT_ID, ACTIVITY_ID, EnumActivityCertificateTemplateType.GUEST))
                .thenReturn(activityTemplate(EnumActivityCertificateTemplateType.GUEST));
        when(variablesServices.loadActivityRoleVariables(List.of(), ACTIVITY_ID, RECIPIENT, EnumCertificateType.GUEST))
                .thenReturn(Map.of());

        assertThat(services.buildActivityCertificateFromGuest(EVENT_ID, ACTIVITY_ID, GUEST_ID, RECIPIENT)
                .contentType()).isEqualTo("image/png");
    }

    @Test
    void buildActivityCertificateFromGuestRejectsUnconfirmedGuest() {
        ActivityDTO activity = activity();
        ActivityGuest link = mock(ActivityGuest.class);
        EventGuest guest = mock(EventGuest.class);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activityGuestRepository.findByActivityIdAndId(ACTIVITY_ID, GUEST_ID)).thenReturn(Optional.of(link));
        when(link.getEventGuest()).thenReturn(guest);
        when(link.getRole()).thenReturn(EnumActivityGuestRole.PRESENTER);
        when(guest.isConfirmed()).thenReturn(false);

        assertThatThrownBy(() -> services.buildActivityCertificateFromGuest(EVENT_ID, ACTIVITY_ID, GUEST_ID, RECIPIENT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void buildCertificateForEditorGeneratesCertificateForOwner() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.isOwner(RECIPIENT)).thenReturn(true);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID, EnumEventCertificateTemplateType.EDITOR)).thenReturn(boxTemplate());
        when(variablesServices.loadEditorVariablesValues(List.of(), EVENT_ID, RECIPIENT)).thenReturn(Map.of());

        assertThat(services.buildCertificateForEditor(EVENT_ID, RECIPIENT).contentType()).isEqualTo("image/png");
    }

    @Test
    void buildCertificateForEditorRejectsNonEditor() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.isOwner(REQUESTER)).thenReturn(false);
        when(accessPolicy.isActiveEditor(EVENT_ID, REQUESTER)).thenReturn(false);

        assertThatThrownBy(() -> services.buildCertificateForEditor(EVENT_ID, REQUESTER))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ActivityDTO activity() {
        ActivityDTO activity = mock(ActivityDTO.class);
        when(activity.eventId()).thenReturn(EVENT_ID);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        return activity;
    }

    private ResolvedActivityCertificateTemplate activityTemplate(EnumActivityCertificateTemplateType context) {
        return new ResolvedActivityCertificateTemplate(context, List.of(), List.of(), cover, true);
    }

    private EventBoxTemplate boxTemplate() {
        return EventBoxTemplate.builder().eventId(EVENT_ID).variables(List.of()).boxes(List.of()).build();
    }

    private StorageExternal.StoredFile createCover() throws Exception {
        BufferedImage image = new BufferedImage(320, 160, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return new StorageExternal.StoredFile(new ByteArrayResource(bytes.toByteArray()), "image/png");
    }
}
