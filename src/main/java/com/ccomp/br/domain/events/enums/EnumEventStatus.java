package com.ccomp.br.domain.events.enums;

public enum EnumEventStatus {
    DRAFT,       // Rascunho - visível apenas para organizadores/editores
    PUBLISHED,   // Público - visível nas buscas e listagens gerais
    UNLISTED,    // Oculto/Privado - acessível apenas via ID/link direto
    CANCELED     // Cancelado - visível porém com operações bloqueadas
}
