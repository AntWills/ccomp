package com.ccomp.br.domain.storage.external;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class StorageKeyGenerator {
    public String generate(String prefix, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();

        return (extension == null || extension.isBlank())
                ? "%s/%s".formatted(prefix, uuid)
                : "%s/%s.%s".formatted(prefix, uuid, extension.toLowerCase());
    }
}
