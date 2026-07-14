package com.ccomp.br.domain.storage.application;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.endpoint}")
    private String endpoint;

    public String upload(MultipartFile file, String fileName) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            return endpoint + "/" + bucket + "/" + fileName;
        } catch (IOException | SdkException e) {
            log.error("Não foi possível enviar o arquivo {} para o bucket {}!", fileName, bucket);
            log.error(e.getMessage());
            return null;
        }
    }

    public Resource findByFileName(String fileName) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request ->
                    request.bucket(bucket).key(fileName));

            return new ByteArrayResource(response.asByteArray());
        } catch (SdkException e) {
            log.error("Não foi possível encontrar o arquivo {} no bucket {}!", fileName, bucket);
            log.error(e.getMessage());
            return null;
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
            log.error("Não foi possível deletar o arquivo {} do bucket {}!", fileName, bucket);
            log.error(e.getMessage());
        }
    }
}