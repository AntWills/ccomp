package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.issuance.ManualCertificateServices;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.ManualCertificateContentRequest;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualCertificateContentServicesTest {
    private static final Long EVENT_ID = 50L;
    private static final UUID MANAGER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");

    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;
    @Mock CertificateFetcher certificateFetcher;
    @Mock CertificateVariablesServices variablesServices;
    @Mock ActivityCache activityCache;
    @Mock EventInstructorRepository instructorRepository;
    @Mock ActivityGuestRepository activityGuestRepository;

    private ManualCertificateServices services;
    private ManualCertificateContentRequest request;
    private EventDTO finishedEvent;
    private StorageExternal.StoredFile cover;

    @BeforeEach
    void setUp() throws Exception {
        services = new ManualCertificateServices(eventCache, accessPolicy, certificateFetcher,
                variablesServices, activityCache, instructorRepository, activityGuestRepository);
        request = new ManualCertificateContentRequest(
                List.of(new BoxElementDTO("{recipient}", 0.1, 0.2, 0.8, 32f, "#000000", null)),
                Map.of("{recipient}", "Ana Souza"));
        finishedEvent = event(EnumEventStatus.PUBLISHED, EnumEventExecutionStatus.FINISHED);
        cover = createCover();
    }

    @Test
    void previewUsesClientProvidedBoxesAndValuesAndReturnsPng() throws Exception {
        allowEdit(finishedEvent);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);

        StorageExternal.StoredFile result = services.previewCustomCertificate(EVENT_ID, MANAGER_ID, request);

        BufferedImage image = ImageIO.read(result.resource().getInputStream());
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(500);
        assertThat(image.getHeight()).isEqualTo(200);
        assertThat(countNonWhitePixels(image)).isPositive();
        verify(certificateFetcher).fetchCoverTemplate(EVENT_ID);
    }

    @Test
    void definitiveCertificateIsGeneratedForFinishedEvent() {
        allowEdit(finishedEvent);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(cover);

        StorageExternal.StoredFile result = services.issueCustomCertificate(EVENT_ID, MANAGER_ID, request);

        assertThat(result.contentType()).isEqualTo("image/png");
        verify(certificateFetcher).fetchCoverTemplate(EVENT_ID);
    }

    @Test
    void definitiveCertificateIsRejectedBeforeEventFinishes() {
        EventDTO ongoingEvent = event(EnumEventStatus.PUBLISHED, EnumEventExecutionStatus.IN_PROGRESS);
        allowEdit(ongoingEvent);

        assertThatThrownBy(() -> services.issueCustomCertificate(EVENT_ID, MANAGER_ID, request))
                .isInstanceOf(DomainException.class)
                .hasMessage("O certificado definitivo só pode ser emitido após o encerramento do evento.");
        verify(certificateFetcher, never()).fetchCoverTemplate(EVENT_ID);
    }

    @Test
    void definitiveCertificateIsRejectedForCanceledEvent() {
        EventDTO canceledEvent = event(EnumEventStatus.CANCELED, EnumEventExecutionStatus.FINISHED);
        allowEdit(canceledEvent);

        assertThatThrownBy(() -> services.issueCustomCertificate(EVENT_ID, MANAGER_ID, request))
                .isInstanceOf(DomainException.class);
        verify(certificateFetcher, never()).fetchCoverTemplate(EVENT_ID);
    }

    @Test
    void previewRequiresEventEditPermission() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(finishedEvent));
        when(accessPolicy.canEdit(finishedEvent, MANAGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> services.previewCustomCertificate(EVENT_ID, MANAGER_ID, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(certificateFetcher, never()).fetchCoverTemplate(EVENT_ID);
    }

    private EventDTO event(EnumEventStatus status, EnumEventExecutionStatus executionStatus) {
        return new EventDTO(EVENT_ID, "Evento", "evento", null, null, null,
                null, null, null, status,
                null, null, executionStatus,
                null, null, null, null, MANAGER_ID, null, null, "Auditório",
                null, null, null);
    }

    private void allowEdit(EventDTO event) {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, MANAGER_ID)).thenReturn(true);
    }

    private StorageExternal.StoredFile createCover() throws Exception {
        BufferedImage image = new BufferedImage(500, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return new StorageExternal.StoredFile(new ByteArrayResource(bytes.toByteArray()), "image/png");
    }

    private long countNonWhitePixels(BufferedImage image) {
        long count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != Color.WHITE.getRGB()) count++;
            }
        }
        return count;
    }
}
