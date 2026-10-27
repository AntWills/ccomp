package com.ccomp.br.module.certificate;

import com.ccomp.br.domain.storage.external.StorageExternal;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.AttributedCharacterIterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateGenerationTest {

    @Test
    void generate_rendersReplacedUnderlinedTextIntoPng() throws Exception {
        BufferedImage cover = new BufferedImage(500, 180, BufferedImage.TYPE_INT_RGB);
        var graphics = cover.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, cover.getWidth(), cover.getHeight());
        graphics.dispose();
        var coverBytes = new ByteArrayOutputStream();
        ImageIO.write(cover, "png", coverBytes);

        StorageExternal.StoredFile generated = Certificate.builder()
                .coverImage(new StorageExternal.StoredFile(
                        new ByteArrayResource(coverBytes.toByteArray()), "image/png"))
                .boxElements(List.of(new BoxElement(
                        "__{participante.nome}__", 0.1, 0.2, 0.8, 36f, "#000000", EnumAlignment.LEFT)))
                .toReplace(Map.of("{participante.nome}", "Ana Souza"))
                .build()
                .generate();

        BufferedImage output = ImageIO.read(generated.resource().getInputStream());
        assertThat(generated.contentType()).isEqualTo("image/png");
        assertThat(output).isNotNull();
        assertThat(output.getWidth()).isEqualTo(500);
        assertThat(output.getHeight()).isEqualTo(180);
        assertThat(countNonWhitePixels(output)).isGreaterThan(0);
    }

    @Test
    void parse_appliesUnderlineToFormattedRange() {
        var parsed = MarkdownTextParser.parse("antes __sublinhado__ depois",
                new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 20), Color.BLACK);
        AttributedCharacterIterator iterator = parsed.getIterator();
        iterator.setIndex("antes ".length());

        assertThat(iterator.getAttribute(java.awt.font.TextAttribute.UNDERLINE))
                .isEqualTo(java.awt.font.TextAttribute.UNDERLINE_ON);
        iterator.setIndex(0);
        assertThat(iterator.getAttribute(java.awt.font.TextAttribute.UNDERLINE)).isNull();
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
