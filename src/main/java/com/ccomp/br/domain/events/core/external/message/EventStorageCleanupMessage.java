package com.ccomp.br.domain.events.core.external.message;

import java.util.List;

public record EventStorageCleanupMessage(List<String> fileKeys) {}
