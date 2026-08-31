package com.ccomp.br.domain.events.enums;

public enum EnumEnrollmentStatus {
    UPCOMING,   // Em breve - data de abertura no futuro
    OPEN,       // Inscrições abertas
    PAUSED,     // Pausado manualmente pelo organizador
    SOLD_OUT,   // Vagas esgotadas
    CLOSED      // Encerrado - prazo final ultrapassado ou evento cancelado/rascunho
}
