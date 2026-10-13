package com.ccomp.br.domain.events.certificate.dto;

import java.util.List;

/**
 * Template de caixas em uso (evento ou atividade), espelhando o payload aceito
 * no salvamento para permitir ida e volta no editor.
 */
public record BoxTemplateResponse(
        List<BoxElementDTO> boxes,
        List<String> variables
) {
}
