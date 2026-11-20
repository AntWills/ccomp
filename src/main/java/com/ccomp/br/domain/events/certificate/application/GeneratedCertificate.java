package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.storage.external.StorageExternal;

public record GeneratedCertificate(StorageExternal.StoredFile file, String fileName) {
}
