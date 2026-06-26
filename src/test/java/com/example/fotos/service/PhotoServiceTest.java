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
    void upload_savesArtworkWithAllFields() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "painting.jpg", "image/jpeg", "fake-image-bytes".getBytes());
        when(storageService.store(file)).thenReturn("uuid-painting.jpg");

        photoService.upload(file, "Starry Night", "Van Gogh", "A swirling night sky");

        verify(photoRepository).save(argThat(photo ->
                "uuid-painting.jpg".equals(photo.getS3Key()) &&
                "Starry Night".equals(photo.getTitle()) &&
                "Van Gogh".equals(photo.getArtist()) &&
                "A swirling night sky".equals(photo.getDescription())
        ));
    }

    @Test
    void upload_throwsWhenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> photoService.upload(empty, "Title", "Artist", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void upload_throwsWhenTitleIsBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> photoService.upload(file, "   ", "Artist", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void upload_throwsWhenArtistIsBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> photoService.upload(file, "Title", "  ", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist");
    }

    @Test
    void upload_throwsWhenDescriptionIsBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> photoService.upload(file, "Title", "Artist", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_cleansUpS3KeyWhenSaveFails() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", new byte[]{1});
        when(storageService.store(file)).thenReturn("uuid-img.jpg");
        when(photoRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> photoService.upload(file, "Title", "Artist", "desc"))
                .isInstanceOf(RuntimeException.class);
        verify(storageService).delete("uuid-img.jpg");
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
    void findAll_delegatesToRepositorySortedMethod() {
        Photo a = makePhoto("a");
        Photo b = makePhoto("b");
        when(photoRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(b, a));

        List<Photo> result = photoService.findAll();

        assertThat(result).extracting(Photo::getS3Key).containsExactly("b", "a");
    }

    private Photo makePhoto(String key) {
        Photo p = new Photo();
        p.setTitle("Title");
        p.setArtist("Artist");
        p.setS3Key(key);
        p.setDescription("desc");
        return p;
    }
}
