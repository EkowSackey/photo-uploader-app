package com.example.fotos.service;

import com.example.fotos.config.S3Properties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
        validateFileType(file);

        String originalFilename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String key = UUID.randomUUID() + "-" + originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return key;
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build());
    }

    private void validateFileType(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted");
        }

        byte[] header = file.getInputStream().readNBytes(12);
        if (!isImage(header)) {
            throw new IllegalArgumentException("Only image files are accepted");
        }
    }

    private boolean isImage(byte[] h) {
        if (h.length < 4) return false;

        // JPEG: FF D8 FF
        if (h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF) return true;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47) return true;

        // GIF: 47 49 46 38 (GIF8)
        if (h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x38) return true;

        // WebP: RIFF (4 bytes) + size (4 bytes) + WEBP
        if (h.length >= 12 &&
            h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46 &&
            h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50) return true;

        return false;
    }
}
