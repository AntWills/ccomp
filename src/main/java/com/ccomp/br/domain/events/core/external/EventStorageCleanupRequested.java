package com.ccomp.br.domain.events.core.external;

import java.util.List;

public record EventStorageCleanupRequested(List<String> fileKeys) {}
