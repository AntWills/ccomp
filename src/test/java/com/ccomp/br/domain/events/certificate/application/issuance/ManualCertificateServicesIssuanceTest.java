package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.ManualInstructorCertificateRequest;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualCertificateServicesIssuanceTest {
    private static final long EVENT_ID = 50L;
    private static final long INSTRUCTOR_ID = 60L;
    private static final long ACTIVITY_ID = 70L;
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();

    @Mock private EventCache eventCache;
    @Mock private EventAccessPolicy accessPolicy;
    @Mock private CertificateFetcher certificateFetcher;
    @Mock private CertificateVariablesServices variablesServices;
    @Mock private ActivityCache activityCache;
    @Mock private EventInstructorRepository instructorRepository;
    @Mock private ActivityGuestRepository activityGuestRepository;

    private ManualCertificateServices services;
    private EventDTO event;
    private StorageExternal.StoredFile cover;

    @BeforeEach
    void setUp() throws Exception {
        services = new ManualCertificateServices(eventCache, accessPolicy, certificateFetcher,
                variablesServices, activityCache, instructorRepository, activityGuestRepository);
        event = mock(EventDTO.class);
        cover = createCover();
    }

    @Test
    void buildInstructorCertificateGeneratesCertificateForConfirmedInstructor() {
        EventInstructor instructor = EventInstructor.builder()
                .id(INSTRUCTOR_ID)
                .activityId(ACTIVITY_ID)
                .userId(RECIPIENT_ID)
                .name("Alex")
                .build();
        ActivityDTO activity = activity();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(instructorRepository.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(instructor));
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(variablesServices.sumInstructorWorkload(RECIPIENT_ID, EVENT_ID)).thenReturn(BigDecimal.ONE);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID, EnumEventCertificateTemplateType.INSTRUCTOR))
                .thenReturn(boxTemplate());
        when(variablesServices.loadManualInstructorVariablesValues(EVENT_ID, "Alex", BigDecimal.ONE))
                .thenReturn(Map.of());

        var result = services.buildInstructorCertificate(EVENT_ID,
                new ManualInstructorCertificateRequest(INSTRUCTOR_ID, null, null), MANAGER_ID);

        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void buildInstructorCertificateFailsWhenInstructorDoesNotExist() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(instructorRepository.findById(INSTRUCTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> services.buildInstructorCertificate(EVENT_ID,
                new ManualInstructorCertificateRequest(INSTRUCTOR_ID, null, null), MANAGER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buildTeamCertificateGeneratesCertificateForEventTeamMember() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(event.isOwner(RECIPIENT_ID)).thenReturn(true);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID, EnumEventCertificateTemplateType.EDITOR))
                .thenReturn(boxTemplate());
        when(variablesServices.loadEditorVariablesValues(List.of(), EVENT_ID, RECIPIENT_ID)).thenReturn(Map.of());

        assertThat(services.buildTeamCertificate(EVENT_ID, RECIPIENT_ID, MANAGER_ID).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void buildTeamCertificateRejectsRequesterWithoutEditPermission() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> services.buildTeamCertificate(EVENT_ID, RECIPIENT_ID, MANAGER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ActivityDTO activity() {
        ActivityDTO activity = mock(ActivityDTO.class);
        when(activity.eventId()).thenReturn(EVENT_ID);
        return activity;
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
