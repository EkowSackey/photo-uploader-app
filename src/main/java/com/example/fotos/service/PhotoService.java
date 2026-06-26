package com.example.fotos.service;

import com.example.fotos.config.S3Properties;
import com.example.fotos.entity.Photo;
import com.example.fotos.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        return photoRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void upload(MultipartFile file, String title, String artist, String description) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Title is required");
        }

        if (!StringUtils.hasText(artist)) {
            throw new IllegalArgumentException("Artist is required");
        }

        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("Description is required");
        }

        String key = storageService.store(file);

        try {
            Photo photo = new Photo();
            photo.setTitle(title.trim());
            photo.setArtist(artist.trim());
            photo.setS3Key(key);
            photo.setDescription(description.trim());
            photoRepository.save(photo);
        } catch (Exception e) {
            storageService.delete(key);
            throw e;
        }
    }

    public Photo findById(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
    }

    @Transactional
    public void deleteById(Long id) {
        Photo photo = findById(id);
        storageService.delete(photo.getS3Key());
        photoRepository.deleteById(id);
    }

    @Transactional
    public void update(Long id, String title, String artist, String description) {
        if (!StringUtils.hasText(title)) throw new IllegalArgumentException("Title is required");
        if (!StringUtils.hasText(artist)) throw new IllegalArgumentException("Artist is required");
        if (!StringUtils.hasText(description)) throw new IllegalArgumentException("Description is required");
        Photo photo = findById(id);
        photo.setTitle(title.trim());
        photo.setArtist(artist.trim());
        photo.setDescription(description.trim());
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
