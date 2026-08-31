package com.ccomp.br.domain.events.enums;

public enum EnumEnrollmentState {
//    PENDING,     // Inscrição pendente (aguardando aprovação manual ou confirmação)
    CONFIRMED,   // Inscrição ativa e confirmada
//    WAITLIST,    // Em lista de espera (quando ultrapassa a capacidade)
    CHECKED_IN,  // Presença confirmada no evento (fundamental para emissão de certificados)
    CANCELED     // Inscrição cancelada pelo participante ou pelo organizador
}
