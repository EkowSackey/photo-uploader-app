package com.example.fotos.service;

import com.example.fotos.config.S3Properties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Profile("!local")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3StorageService(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String key = UUID.randomUUID() + "-" + originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return key;
    }
}
