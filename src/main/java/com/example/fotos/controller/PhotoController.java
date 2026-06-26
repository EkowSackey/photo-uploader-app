package com.example.fotos.controller;

import com.example.fotos.entity.Photo;
import com.example.fotos.service.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public String gallery(Model model) {
        List<PhotoView> photos = toViews(photoService.findAll());
        model.addAttribute("photos", photos);
        return "gallery";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("title") String title,
                         @RequestParam("artist") String artist,
                         @RequestParam("description") String description,
                         Model model) {
        try {
            photoService.upload(file, title, artist, description);
            return "redirect:/";
        } catch (IllegalArgumentException | IOException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("artist", artist);
            model.addAttribute("description", description);
            return "upload";
        }
    }

    @PostMapping("/photos/{id}/delete")
    public String delete(@PathVariable Long id) {
        photoService.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/photos/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("photo", photoService.findById(id));
        return "edit";
    }

    @PostMapping("/photos/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String artist,
                       @RequestParam String description,
                       Model model) {
        try {
            photoService.update(id, title, artist, description);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("photo", photoService.findById(id));
            model.addAttribute("error", e.getMessage());
            return "edit";
        }
    }

    private List<PhotoView> toViews(List<Photo> photos) {
        return photos.stream()
                .map(p -> new PhotoView(
                        p.getId(),
                        p.getTitle(),
                        p.getArtist(),
                        p.getDescription(),
                        photoService.buildImageUrl(p.getS3Key())
                ))
                .toList();
    }

    public record PhotoView(Long id, String title, String artist, String description, String imageUrl) {
    }
}
