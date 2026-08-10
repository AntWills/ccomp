package com.ccomp.br.domain.storage.application;

import com.ccomp.br.domain.storage.dto.UploadFileResponse;
import com.ccomp.br.shared.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.endpoint}")
    private String endpoint;

    public UploadFileResponse upload(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (IOException | SdkException e) {
            log.error("Não foi possível enviar o arquivo [{}] para o bucket [{}]!", fileName, bucket, e);
            throw new StorageException("Falha ao salvar arquivo no storage: " + e.getMessage());
        }
        return new UploadFileResponse(endpoint + "/" + bucket + "/" + fileName, fileName);
    }

    public Optional<Resource> findByFileName(String fileName) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request ->
                    request.bucket(bucket).key(fileName));

            return Optional.of(new ByteArrayResource(response.asByteArray()));
        } catch (SdkException e) {
            log.error("Não foi possível encontrar o arquivo [{}] no bucket [{}]!", fileName, bucket);
            log.error(e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String fileName) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
                            .build());
        } catch (SdkException e) {
            log.error("Não foi possível deletar o arquivo [{}] do bucket [{}]!", fileName, bucket);
            log.error(e.getMessage());
        }
    }
}