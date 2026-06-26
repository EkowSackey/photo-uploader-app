package com.example.fotos.controller;

import com.example.fotos.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhotoController.class)
class PhotoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PhotoService photoService;

    @Test
    void gallery_returns200WithPhotosInModel() throws Exception {
        when(photoService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery"))
                .andExpect(model().attributeExists("photos"));
    }

    @Test
    void uploadForm_returns200() throws Exception {
        mockMvc.perform(get("/upload"))
                .andExpect(status().isOk())
                .andExpect(view().name("upload"));
    }

    @Test
    void upload_redirectsToGalleryOnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "art.jpg", "image/jpeg", "fake".getBytes());
        doNothing().when(photoService).upload(any(), anyString(), anyString(), anyString());

        mockMvc.perform(multipart("/upload")
                        .file(file)
                        .param("title", "Starry Night")
                        .param("artist", "Van Gogh")
                        .param("description", "A swirling sky"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void upload_returnsUploadViewWithErrorWhenFileIsEmpty() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        doThrow(new IllegalArgumentException("Image file is required"))
                .when(photoService).upload(any(), anyString(), anyString(), anyString());

        mockMvc.perform(multipart("/upload")
                        .file(empty)
                        .param("title", "Title")
                        .param("artist", "Artist")
                        .param("description", "desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("upload"))
                .andExpect(model().attributeExists("error"));
    }
}
