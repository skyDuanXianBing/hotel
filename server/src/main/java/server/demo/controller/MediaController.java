package server.demo.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.MediaUploadResponseDTO;
import server.demo.service.MediaStorageService;
import server.demo.util.StoreContextUtils;

/**
 * 媒体上传与公开访问接口。
 */
@RestController
public class MediaController {

    private final MediaStorageService mediaStorageService;

    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @StoreScoped
    @PostMapping("/api/v1/media/upload")
    public ApiResponse<MediaUploadResponseDTO> upload(
            @RequestParam("scope") String scope,
            @RequestParam("file") MultipartFile file
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        return ApiResponse.success("上传成功", mediaStorageService.upload(storeId, scope, file));
    }

    @GetMapping("/media/{storeId}/{scope}/{filename:.+}")
    public ResponseEntity<FileSystemResource> read(
            @PathVariable Long storeId,
            @PathVariable String scope,
            @PathVariable String filename
    ) {
        FileSystemResource resource = mediaStorageService.loadAsResource(storeId, scope, filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
