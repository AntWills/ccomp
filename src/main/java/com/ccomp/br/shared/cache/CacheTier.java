package com.ccomp.br.shared.cache;

public enum CacheTier {
    VOLATILE, // listagens com filtro, dados que mudam o tempo todo
    SHORT,    // entidades com escrita moderada
    MEDIUM,   // pouca escrita
    LONG      // quase estático
}
