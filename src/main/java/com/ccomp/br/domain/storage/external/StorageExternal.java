package com.ccomp.br.domain.storage.external;

import com.ccomp.br.shared.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Optional;

@Component
public class StorageExternal {
    private final S3Client s3Client;

    @Value("${storage.bucket}")
    private String bucketName;

    public StorageExternal(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Envia o arquivo diretamente pelo backend (proxy de bytes).
     * Simples e suficiente para arquivos pequenos como imagens de capa.
     */
    public void putObject(String fileKey, MultipartFile file) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new StorageException("Erro ao enviar arquivo para o storage.\nErr:" + e.getMessage());
        }
    }

    /**
     * Busca o objeto e devolve como Resource pronto para stream na resposta HTTP,
     * junto do content-type original salvo no upload.
     */
    public Optional<StoredFile> getObject(String fileKey) {
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .build()
            );
            String contentType = s3Object.response().contentType();
            return Optional.of(new StoredFile(new InputStreamResource(s3Object), contentType));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    public void deleteObject(String fileKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build());
    }

    public record StoredFile(Resource resource, String contentType) {}
}