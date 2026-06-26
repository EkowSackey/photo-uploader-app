package com.example.fotos.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@Profile("local")
public class LocalStorageService implements StorageService {

    private static final Logger log = Logger.getLogger(LocalStorageService.class.getName());
    private final Path root = Path.of("uploads");

    @Override
    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(root);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "image";
        String key = UUID.randomUUID() + "-" + originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        Files.copy(file.getInputStream(), root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(root.resolve(key));
        } catch (IOException e) {
            log.warning("Failed to delete local file: " + key);
        }
    }
}
