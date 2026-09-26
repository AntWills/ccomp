package com.ccomp.br.domain.storage.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StorageFileRepository extends JpaRepository<StorageFile, String> {
    Optional<StorageFile> findByFileNameAndOwnerUserId(String fileName, UUID ownerUserId);
}
