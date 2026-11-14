package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.ResolvedActivityCertificateTemplate;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualParticipantCertificateServicesTest {
    private static final Long EVENT_ID = 50L;
    private static final Long ACTIVITY_ID = 70L;
    private static final Long ENROLLMENT_ID = 80L;
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();

    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;
    @Mock CertificateFetcher certificateFetcher;
    @Mock CertificateVariablesServices variablesServices;
    @Mock ActivityCache activityCache;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock EnrollmentActivityRepository enrollmentActivityRepository;

    private ManualParticipantCertificateServices services;
    private EventDTO event;
    private StorageExternal.StoredFile cover;

    @BeforeEach
    void setUp() throws Exception {
        services = new ManualParticipantCertificateServices(eventCache, accessPolicy, certificateFetcher,
                variablesServices, activityCache, enrollmentRepository, enrollmentActivityRepository);
        event = mock(EventDTO.class);
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        cover = new StorageExternal.StoredFile(new ByteArrayResource(bytes.toByteArray()), "image/png");
    }

    @Test
    void issuesEventCertificateForEnrollmentAfterTeamAuthorization() {
        var enrollment = enrollment();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID)).thenReturn(boxTemplate());
        when(variablesServices.loadEventVariablesValues(List.of(), EVENT_ID, RECIPIENT_ID)).thenReturn(Map.of());

        assertThat(services.buildEventCertificate(EVENT_ID, ENROLLMENT_ID, MANAGER_ID).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void rejectsRequesterWithoutEditPermission() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> services.buildEventCertificate(EVENT_ID, ENROLLMENT_ID, MANAGER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsEnrollmentFromAnotherEvent() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        var enrollment = enrollment();
        enrollment.setEventId(999L);
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> services.buildEventCertificate(EVENT_ID, ENROLLMENT_ID, MANAGER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void issuesActivityCertificateForAttendedEnrollment() {
        var activity = mock(ActivityDTO.class);
        when(activity.eventId()).thenReturn(EVENT_ID);
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment()));
        var enrollmentActivity = new EnrollmentActivity();
        enrollmentActivity.setEnrollment(enrollment());
        enrollmentActivity.setAttendedAt(LocalDateTime.now());
        when(enrollmentActivityRepository.findByEnrollmentIdAndActivityId(ENROLLMENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(enrollmentActivity));
        var resolved = mock(ResolvedActivityCertificateTemplate.class);
        when(certificateFetcher.resolveActivityTemplate(EVENT_ID, ACTIVITY_ID,
                EnumActivityCertificateTemplateType.PARTICIPANT)).thenReturn(resolved);
        when(resolved.cover()).thenReturn(cover);
        when(resolved.boxes()).thenReturn(List.of());
        when(resolved.variables()).thenReturn(List.of());
        when(variablesServices.loadActivityVariablesValues(List.of(), ACTIVITY_ID, RECIPIENT_ID)).thenReturn(Map.of());

        assertThat(services.buildActivityCertificate(EVENT_ID, ACTIVITY_ID, ENROLLMENT_ID, MANAGER_ID).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void rejectsActivityEnrollmentWithoutAttendance() {
        var activity = mock(ActivityDTO.class);
        when(activity.eventId()).thenReturn(EVENT_ID);
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
        when(activityCache.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment()));
        when(enrollmentActivityRepository.findByEnrollmentIdAndActivityId(ENROLLMENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> services.buildActivityCertificate(EVENT_ID, ACTIVITY_ID, ENROLLMENT_ID, MANAGER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Enrollment enrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setEventId(EVENT_ID);
        enrollment.setUserId(RECIPIENT_ID);
        enrollment.setStatus(EnumEnrollmentState.CHECKED_IN);
        return enrollment;
    }

    private EventBoxTemplate boxTemplate() {
        return EventBoxTemplate.builder().boxes(List.of()).variables(List.of()).build();
    }
}
