package com.ccomp.br.module.certificate;

import com.ccomp.br.domain.storage.external.StorageExternal;
import lombok.Builder;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
public class Certificate {
    private static final float DEFAULT_FONT_SIZE = 40.0f;
    private static final Color DEFAULT_FONT_COLOR = Color.BLACK;
    private static final EnumAlignment DEFAULT_ALIGNMENT = EnumAlignment.LEFT;
    private static final float LINE_SPACING = 1.2f;
    private static final long MAX_IMAGE_PIXELS = 30_000_000L;

    private StorageExternal.StoredFile coverImage;
    private List<BoxElement> boxElements;
    private Map<String, String> toReplace;

    public StorageExternal.StoredFile generate() {
        if (coverImage == null || coverImage.resource() == null) {
            throw new IllegalArgumentException("A imagem de fundo é obrigatória.");
        }

        try (InputStream input = coverImage.resource().getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem suportada.");
            }
            if ((long) image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS) {
                throw new IllegalArgumentException("A imagem excede o limite de resolução para a prévia.");
            }

            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                if (boxElements != null) {
                    for (BoxElement element : boxElements) {
                        drawTextBox(graphics, image, element, applyReplacements(element.textTemplate(), toReplace));
                    }
                }
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("Não foi possível codificar a imagem da prévia em PNG.");
            }
            return new StorageExternal.StoredFile(new ByteArrayResource(output.toByteArray()), "image/png");
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler ou gerar a imagem da prévia.", e);
        }
    }
    private void drawTextBox(Graphics2D graphics, BufferedImage image, BoxElement element, String text) {
        if (element == null || element.x() == null || element.y() == null
                || element.width() == null || element.height() == null) {
            throw new IllegalArgumentException("Cada caixa deve informar posição, largura e altura.");
        }

        float fontSize = element.fontSize() != null ? element.fontSize() : DEFAULT_FONT_SIZE;
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize).deriveFont(fontSize);
        Color fontColor = parseColor(element.fontColor());
        EnumAlignment alignment = element.alignment() != null ? element.alignment() : DEFAULT_ALIGNMENT;

        int x = (int) Math.round(element.x() * image.getWidth());
        int y = (int) Math.round(element.y() * image.getHeight());
        float boxWidth = (float) (element.width() * image.getWidth());
        float boxHeight = (float) (element.height() * image.getHeight());

        Graphics2D boxGraphics = (Graphics2D) graphics.create();
        boxGraphics.clipRect(x, y, Math.round(boxWidth), Math.round(boxHeight));

        // 1. Converte a string com Markdown básico para AttributedString
        AttributedString attributedText = MarkdownTextParser.parse(text, font, fontColor);
        AttributedCharacterIterator iterator = attributedText.getIterator();
        String renderedText = getIteratorText(iterator);

        // 2. Quebra de linha e renderização nativa
        LineBreakMeasurer measurer = new LineBreakMeasurer(iterator, boxGraphics.getFontRenderContext());
        float lineHeight = fontSize * LINE_SPACING;
        float currentY = y + fontSize;

        try {
            while (measurer.getPosition() < iterator.getEndIndex()) {
                int nextPos = measurer.nextOffset(boxWidth);
                int limit = measurer.getPosition();

                // Tratamento para quebras de linha explícitas (\n) no texto
                String sub = renderedText.substring(limit, nextPos);
                int newLineIdx = sub.indexOf('\n');
                if (newLineIdx != -1) {
                    nextPos = limit + newLineIdx + 1;
                }

                TextLayout layout = measurer.nextLayout(boxWidth, nextPos, false);
                if (layout == null) break;

                float drawX = x;
                boolean isLastLine = measurer.getPosition() >= iterator.getEndIndex();

                switch (alignment) {
                    case JUSTIFIED -> {
                        if (!isLastLine && layout.getAdvance() < boxWidth) {
                            layout = layout.getJustifiedLayout(boxWidth);
                        }
                    }
                    case RIGHT -> drawX = x + (boxWidth - layout.getAdvance());
                    case CENTERED -> drawX = x + ((boxWidth - layout.getAdvance()) / 2.0f);
                    case LEFT -> drawX = x;
                }

                layout.draw(boxGraphics, drawX, currentY);
                currentY += lineHeight;
            }
        } finally {
            boxGraphics.dispose();
        }
    }

    private String getIteratorText(AttributedCharacterIterator iterator) {
        StringBuilder text = new StringBuilder(iterator.getEndIndex() - iterator.getBeginIndex());
        for (char character = iterator.first(); character != AttributedCharacterIterator.DONE; character = iterator.next()) {
            text.append(character);
        }
        return text.toString();
    }

    private List<String> wrap(String text, float maxWidth, Font font, FontRenderContext context) {
        if (maxWidth <= 0) {
            throw new IllegalArgumentException("A largura da caixa de texto precisa ser maior que zero.");
        }

        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (new TextLayout(word, font, context).getAdvance() > maxWidth) {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    splitLongWord(word, maxWidth, font, context, lines, line);
                    continue;
                }

                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && new TextLayout(candidate, font, context).getAdvance() > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void splitLongWord(String word, float maxWidth, Font font, FontRenderContext context,
                               List<String> lines, StringBuilder remainder) {
        StringBuilder chunk = new StringBuilder();
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String candidate = chunk.toString() + new String(Character.toChars(codePoint));
            if (chunk.isEmpty() && new TextLayout(candidate, font, context).getAdvance() > maxWidth) {
                throw new IllegalArgumentException("A caixa é estreita demais para exibir um caractere da fonte.");
            }
            if (!chunk.isEmpty() && new TextLayout(candidate, font, context).getAdvance() > maxWidth) {
                lines.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        remainder.append(chunk);
    }

    private Color parseColor(String hexColor) {
        if (hexColor == null || hexColor.isBlank()) {
            return DEFAULT_FONT_COLOR;
        }
        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            if (hex.length() == 8) {
                int argb = (int) Long.parseLong(hex, 16);
                return new Color(argb, true);
            }
            return Color.decode(hexColor.startsWith("#") ? hexColor : "#" + hexColor);
        } catch (NumberFormatException e) {
            return DEFAULT_FONT_COLOR;
        }
    }

    private String applyReplacements(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) return template == null ? "" : template;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
