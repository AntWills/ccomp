package com.ccomp.br.module.certificate;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownTextParser {

    // Regex para identificar __sublinhado__, ~~riscado~~, **negrito** e *itálico*
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(
            "(__(?<underline>.*?)__)|(~~(?<strike>.*?)~~)|(\\*\\*(?<bold>.*?)\\*\\*)|(\\*(?<italic>.*?)\\*)"
    );

    public static AttributedString parse(String markdownText, Font baseFont, Color fontColor) {
        if (markdownText == null || markdownText.isBlank()) {
            AttributedString empty = new AttributedString(" ");
            empty.addAttribute(TextAttribute.FONT, baseFont);
            empty.addAttribute(TextAttribute.FOREGROUND, fontColor);
            return empty;
        }

        StringBuilder plainText = new StringBuilder();
        List<StyleRange> ranges = new ArrayList<>();

        Matcher matcher = MARKDOWN_PATTERN.matcher(markdownText);
        int lastIndex = 0;

        while (matcher.find()) {
            // Adiciona o texto normal antes do elemento formatado
            plainText.append(markdownText, lastIndex, matcher.start());

            int start = plainText.length();
            String content = "";
            boolean isBold = false;
            boolean isItalic = false;
            boolean isStrike = false;
            boolean isUnderline = false;

            if (matcher.group("underline") != null) {
                content = matcher.group("underline");
                isUnderline = true;
            } else if (matcher.group("strike") != null) {
                content = matcher.group("strike");
                isStrike = true;
            } else if (matcher.group("bold") != null) {
                content = matcher.group("bold");
                isBold = true;
            } else if (matcher.group("italic") != null) {
                content = matcher.group("italic");
                isItalic = true;
            }

            plainText.append(content);
            int end = plainText.length();

            if (start < end) {
                ranges.add(new StyleRange(start, end, isBold, isItalic, isStrike, isUnderline));
            }

            lastIndex = matcher.end();
        }

        // Adiciona o restante do texto
        plainText.append(markdownText.substring(lastIndex));

        String result = plainText.isEmpty() ? " " : plainText.toString();
        AttributedString attributedString = new AttributedString(result);

        // Configuração de estilo base
        attributedString.addAttribute(TextAttribute.FONT, baseFont);
        attributedString.addAttribute(TextAttribute.FOREGROUND, fontColor);

        // Aplicação dos estilos por trecho
        for (StyleRange range : ranges) {
            if (range.bold() || range.italic()) {
                int fontStyle = baseFont.getStyle();
                if (range.bold()) {
                    fontStyle |= Font.BOLD;
                }
                if (range.italic()) {
                    fontStyle |= Font.ITALIC;
                }
                attributedString.addAttribute(
                        TextAttribute.FONT,
                        baseFont.deriveFont(fontStyle),
                        range.start(),
                        range.end()
                );
            }
            if (range.strikethrough()) {
                attributedString.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, range.start(), range.end());
            }
            if (range.underline()) {
                attributedString.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, range.start(), range.end());
            }
        }

        return attributedString;
    }

    private record StyleRange(int start, int end, boolean bold, boolean italic, boolean strikethrough,
                              boolean underline) {}
}
