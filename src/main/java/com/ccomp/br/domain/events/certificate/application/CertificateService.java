package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.storage.external.StorageExternal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {
    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");

    public StorageExternal.StoredFile loadCoverTemplate (long eventId) {
        return new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);
    }
}
