package com.example.fotos.service;

import com.example.fotos.config.S3Properties;
import com.example.fotos.entity.Photo;
import com.example.fotos.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final S3Properties s3Properties;
    private final StorageService storageService;

    public PhotoService(PhotoRepository photoRepository,
                        S3Properties s3Properties,
                        StorageService storageService) {
        this.photoRepository = photoRepository;
        this.s3Properties = s3Properties;
        this.storageService = storageService;
    }

    public List<Photo> findAll() {
        return photoRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public void upload(MultipartFile file, String description) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("Description is required");
        }

        String key = storageService.store(file);

        Photo photo = new Photo();
        photo.setS3Key(key);
        photo.setDescription(description.trim());
        photo.setCreatedAt(OffsetDateTime.now());

        photoRepository.save(photo);
    }

    public String buildImageUrl(String s3Key) {
        String base = s3Properties.getCloudfrontBaseUrl();
        if (base.endsWith("/")) {
            return base + s3Key;
        }
        return base + "/" + s3Key;
    }
}
