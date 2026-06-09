package com.example.fotos.service;

import com.example.fotos.config.S3Properties;
import com.example.fotos.entity.Photo;
import com.example.fotos.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock PhotoRepository photoRepository;
    @Mock StorageService storageService;

    private S3Properties s3Properties;
    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        s3Properties = new S3Properties();
        photoService = new PhotoService(photoRepository, s3Properties, storageService);
    }

    @Test
    void upload_savesPhotoWithCorrectFields() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cat.jpg", "image/jpeg", "fake-image-bytes".getBytes());
        when(storageService.store(file)).thenReturn("uuid-cat.jpg");

        photoService.upload(file, "A cute cat");

        verify(photoRepository).save(argThat(photo ->
                "uuid-cat.jpg".equals(photo.getS3Key()) &&
                "A cute cat".equals(photo.getDescription()) &&
                photo.getCreatedAt() != null
        ));
    }

    @Test
    void upload_throwsWhenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> photoService.upload(empty, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void upload_throwsWhenDescriptionIsBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> photoService.upload(file, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildImageUrl_appendsKeyWithSlash() {
        s3Properties.setCloudfrontBaseUrl("https://d123.cloudfront.net");
        String url = photoService.buildImageUrl("uuid-cat.jpg");
        assertThat(url).isEqualTo("https://d123.cloudfront.net/uuid-cat.jpg");
    }

    @Test
    void buildImageUrl_doesNotDoubleSlash() {
        s3Properties.setCloudfrontBaseUrl("https://d123.cloudfront.net/");
        String url = photoService.buildImageUrl("uuid-cat.jpg");
        assertThat(url).isEqualTo("https://d123.cloudfront.net/uuid-cat.jpg");
    }

    @Test
    void findAll_returnsSortedByCreatedAtDesc() {
        Photo older = makePhoto("a", OffsetDateTime.now().minusDays(2));
        Photo newer = makePhoto("b", OffsetDateTime.now());
        when(photoRepository.findAll()).thenReturn(List.of(older, newer));

        List<Photo> result = photoService.findAll();

        assertThat(result).first().extracting(Photo::getS3Key).isEqualTo("b");
    }

    private Photo makePhoto(String key, OffsetDateTime createdAt) {
        Photo p = new Photo();
        p.setS3Key(key);
        p.setDescription("desc");
        p.setCreatedAt(createdAt);
        return p;
    }
}
