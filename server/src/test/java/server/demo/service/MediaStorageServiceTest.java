package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import server.demo.dto.MediaUploadResponseDTO;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void upload_shouldReturnRelativeMediaUrl() throws IOException {
        MediaStorageService service = new MediaStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                "png".getBytes()
        );

        MediaUploadResponseDTO response = service.upload(7L, "store-desktop", file);

        assertEquals("/media/7/store-desktop/" + response.getUrl().substring(response.getUrl().lastIndexOf('/') + 1), response.getUrl());
        assertTrue(response.getUrl().startsWith("/media/7/store-desktop/"));
    }
}
