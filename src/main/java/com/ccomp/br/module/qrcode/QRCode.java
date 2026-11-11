package com.ccomp.br.module.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class QRCode {
    private static final int QR_SIZE = 300;
    private static final int IMAGE_WIDTH = 320;
    private static final int HORIZONTAL_PADDING = 16;
    private static final int QR_TOP = 12;
    private static final int LABEL_GAP = 0;
    private static final int LINE_HEIGHT = 22;

    public static BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public static BufferedImage generateQRCodeImage(String barcodeText, String label) throws Exception {
        BufferedImage qrImage = generateQRCodeImage(barcodeText);
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 16);

        BufferedImage measuringImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D measuringGraphics = measuringImage.createGraphics();
        measuringGraphics.setFont(font);
        List<String> lines = wrap(label, measuringGraphics.getFontMetrics(), IMAGE_WIDTH - 2 * HORIZONTAL_PADDING);
        measuringGraphics.dispose();

        int imageHeight = QR_TOP + QR_SIZE + LABEL_GAP + lines.size() * LINE_HEIGHT + 12;
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, IMAGE_WIDTH, imageHeight);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(qrImage, (IMAGE_WIDTH - QR_SIZE) / 2, QR_TOP, null);
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);

        FontMetrics metrics = graphics.getFontMetrics();
        int y = QR_TOP + QR_SIZE + LABEL_GAP + metrics.getAscent();
        for (String line : lines) {
            graphics.drawString(line, (IMAGE_WIDTH - metrics.stringWidth(line)) / 2, y);
            y += LINE_HEIGHT;
        }
        graphics.dispose();
        return image;
    }

    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        String label = text == null || text.isBlank() ? "Atividade" : text.trim();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : label.split("\\s+")) {
            if (!line.isEmpty() && metrics.stringWidth(line + " " + word) <= maxWidth) {
                line.append(' ').append(word);
            } else if (line.isEmpty() && metrics.stringWidth(word) <= maxWidth) {
                line.append(word);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (metrics.stringWidth(word) <= maxWidth) {
                    line.append(word);
                } else {
                    for (int index = 0; index < word.length(); index++) {
                        String candidate = line.toString() + word.charAt(index);
                        if (!line.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                            lines.add(line.toString());
                            line.setLength(0);
                        }
                        line.append(word.charAt(index));
                    }
                }
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
