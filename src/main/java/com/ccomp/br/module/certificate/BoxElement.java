package com.ccomp.br.module.certificate;

public record BoxElement(
        String textTemplate,
        Double x,
        Double y,
        Double width,
        Float fontSize,
        String fontColor,
        EnumAlignment alignment
) {
}