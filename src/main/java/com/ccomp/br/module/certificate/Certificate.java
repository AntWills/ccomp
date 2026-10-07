package com.ccomp.br.module.certificate;

import com.ccomp.br.domain.storage.external.StorageExternal;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Builder
public class Certificate {

    private StorageExternal.StoredFile coverImage;
    private List<TextElement> textElements;
    private Map<String, String> toReplace;

    public StorageExternal.StoredFile generate() {
        // Usa try-with-resources para garantir que o InputStream original seja fechado
        try (InputStream is = coverImage.resource().getInputStream()) {

            // 1. Carrega a imagem original (A4 Paisagem) em memória
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("O arquivo fornecido não é uma imagem válida.");
            }

            // 2. Prepara o motor gráfico 2D para desenhar na imagem
            Graphics2D g2d = image.createGraphics();

            // 3. Aplica Anti-aliasing para o texto ficar suave e sem pixels estourados
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 4. Itera sobre cada elemento que precisa ser desenhado
            if (textElements != null) {
                for (TextElement element : textElements) {

                    // Substitui as chaves (ex: {participante.nome}) pelos valores (ex: José Cassios)
                    String finalString = applyReplacements(element.textTemplate(), toReplace);

                    // Configura a fonte e a cor baseando-se no elemento (com fallback padrão)
                    int fontSize = element.fontSize() != null ? element.fontSize() : 40;
                    Color fontColor = element.hexColor() != null ? Color.decode(element.hexColor()) : Color.BLACK;

                    g2d.setFont(new Font("Arial", Font.PLAIN, fontSize));
                    g2d.setColor(fontColor);

                    // Desenha o texto. Importante: no Graphics2D, o X e Y definem a linha de base (baseline) do texto
                    g2d.drawString(finalString, element.x().floatValue(), element.y().floatValue());
                }
            }

            // Libera os recursos do processador gráfico
            g2d.dispose();

            // 5. Converte a imagem modificada de volta para Bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Detecta o formato original baseado no contentType salvo (fallback para jpg)
            String format = (coverImage.contentType() != null && coverImage.contentType().contains("png")) ? "png" : "jpg";
            ImageIO.write(image, format, outputStream);

            // 6. Encapsula os bytes resultantes num ByteArrayResource do Spring
            ByteArrayResource generatedResource = new ByteArrayResource(outputStream.toByteArray());

            // Retorna a nova imagem pronta
            return new StorageExternal.StoredFile(generatedResource, "image/" + format);

        } catch (IOException e) {
            // Em sua arquitetura CCOMP, recomendo trocar por uma exception global tipo "CertificateGenerationException"
            throw new RuntimeException("Falha ao manipular e gerar a imagem do certificado.", e);
        }
    }

    private String applyReplacements(String template, Map<String, String> variables) {
        if (template == null) return "";
        if (variables == null || variables.isEmpty()) return template;

        String processedString = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            // Utiliza o .replace() convencional em vez de .replaceAll() para evitar problemas com regex se a chave tiver chaves "{}"
            processedString = processedString.replace(entry.getKey(), entry.getValue());
        }
        return processedString;
    }

    /**
     * Transformamos o Map antigo num Record mais completo.
     * Isso permite que futuramente você passe o tamanho da fonte e cor customizada pelo BD/Frontend
     * para cada pedaço de texto (ex: O nome pode ser maior que o texto padrão).
     */
    public record TextElement(
            String textTemplate,
            Double x,
            Double y,
            @Nullable
            Integer fontSize, // Opcional
            String hexColor   // Opcional (Ex: "#000000")
    ) {}
}