package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServicesGenerationTest {
    private static final Long EVENT_ID = 42L;
    private static final UUID USER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");

    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;
    @Mock CertificateFetcher certificateFetcher;
    @Mock CertificateVariablesServices variablesServices;

    @Test
    void buildCertificateFromEvent_generatesPngUsingEventTemplateAndVariables() throws Exception {
        EventDTO event = new EventDTO(EVENT_ID, "Evento", "evento", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, USER_ID, null, null, null);
        BufferedImage cover = new BufferedImage(500, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cover.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, cover.getWidth(), cover.getHeight());
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(cover, "png", bytes);

        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canView(event, USER_ID)).thenReturn(true);
        when(certificateFetcher.fetchCoverTemplate(EVENT_ID)).thenReturn(
                new StorageExternal.StoredFile(new ByteArrayResource(bytes.toByteArray()), "image/png"));
        when(certificateFetcher.fetchBoxTemplate(EVENT_ID)).thenReturn(EventBoxTemplate.builder()
                .eventId(EVENT_ID)
                .variables(List.of("{participante.nome}"))
                .boxes(List.of(new BoxElementDTO("__{participante.nome}__", 0.1, 0.2, 0.8,
                        36f, "#000000", null)))
                .build());
        when(variablesServices.loadEventVariablesValues(List.of("{participante.nome}"), EVENT_ID, USER_ID))
                .thenReturn(Map.of("{participante.nome}", "Ana Souza"));

        StorageExternal.StoredFile generated = new CertificateServices(eventCache, accessPolicy,
                certificateFetcher, variablesServices, null).buildCertificateFromEvent(EVENT_ID, USER_ID);

        BufferedImage output = ImageIO.read(generated.resource().getInputStream());
        assertThat(generated.contentType()).isEqualTo("image/png");
        assertThat(output).isNotNull();
        assertThat(output.getWidth()).isEqualTo(500);
        assertThat(output.getHeight()).isEqualTo(180);
        assertThat(countNonWhitePixels(output)).isGreaterThan(0);
    }

    @Test
    void buildCertificateFromEvent_whenEventDoesNotExist_throwsNotFound() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().buildCertificateFromEvent(EVENT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
    }

    @Test
    void buildCertificateFromEvent_whenUserCannotView_throwsAccessDenied() {
        EventDTO event = event();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canView(event, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> createService().buildCertificateFromEvent(EVENT_ID, USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("O usuário não tem acesso a este recurso.");
    }

    private CertificateServices createService() {
        return new CertificateServices(eventCache, accessPolicy, certificateFetcher,
                variablesServices, null);
    }

    private EventDTO event() {
        return new EventDTO(EVENT_ID, "Evento", "evento", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, USER_ID, null, null, null);
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
