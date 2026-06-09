package com.example.fotos.controller;

import com.example.fotos.entity.Photo;
import com.example.fotos.service.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<PhotoView> photos = photoService.findAll()
                .stream()
                .map(photo -> new PhotoView(
                        photo.getId(),
                        photo.getDescription(),
                        photoService.buildImageUrl(photo.getS3Key()),
                        photo.getCreatedAt().toString()
                ))
                .toList();

        model.addAttribute("photos", photos);
        return "index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("description") String description,
                         Model model) {
        try {
            photoService.upload(file, description);
            return "redirect:/";
        } catch (IllegalArgumentException | IOException e) {
            model.addAttribute("error", e.getMessage());

            List<PhotoView> photos = photoService.findAll()
                    .stream()
                    .map(photo -> new PhotoView(
                            photo.getId(),
                            photo.getDescription(),
                            photoService.buildImageUrl(photo.getS3Key()),
                            photo.getCreatedAt().toString()
                    ))
                    .toList();

            model.addAttribute("photos", photos);
            return "index";
        }
    }

    public record PhotoView(Long id, String description, String imageUrl, String createdAt) {
    }
}
