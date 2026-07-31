package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.MediaUploadResponseDTO;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import server.demo.i18n.ApiMessages;
/**
 * 媒体文件上传与读取服务。
 */
@Service
public class MediaStorageService {

    private final Path uploadDir;

    public MediaStorageService(
            @Value("${media.upload.dir:${user.home}/the-host-hub/uploads/media}") String uploadDir
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public MediaUploadResponseDTO upload(Long storeId, String scope, MultipartFile file) {
        if (storeId == null) {
            throw new RuntimeException(ApiMessages.get("api.t.2bfd332b0f72"));
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException(ApiMessages.get("api.t.c7de0f24a299"));
        }
        String normalizedScope = normalizeScope(scope);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException(ApiMessages.get("api.t.284338940052"));
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new RuntimeException(ApiMessages.get("api.t.6ec3e798f0ff"));
        }

        String originalName = file.getOriginalFilename();
        String extension = safeExtension(originalName, contentType);
        String filename = UUID.randomUUID() + extension;
        Path targetDir = uploadDir.resolve(String.valueOf(storeId)).resolve(normalizedScope);
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            file.transferTo(target.toFile());

            MediaUploadResponseDTO response = new MediaUploadResponseDTO();
            response.setUrl("/media/" + storeId + "/" + normalizedScope + "/" + filename);
            response.setOriginalName(originalName);
            response.setContentType(contentType);
            response.setFileSize(file.getSize());
            return response;
        } catch (Exception e) {
            throw new RuntimeException(ApiMessages.get("api.t.332740aed4d6") + e.getMessage(), e);
        }
    }

    public FileSystemResource loadAsResource(Long storeId, String scope, String filename) {
        try {
            String normalizedScope = normalizeScope(scope);
            Path path = uploadDir
                    .resolve(String.valueOf(storeId))
                    .resolve(normalizedScope)
                    .resolve(filename)
                    .normalize();
            if (!path.startsWith(uploadDir) || !Files.exists(path)) {
                throw new NoSuchFileException(filename);
            }
            return new FileSystemResource(path);
        } catch (Exception e) {
            throw new RuntimeException(ApiMessages.get("api.t.ffcf0a1eb083"));
        }
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new RuntimeException(ApiMessages.get("api.t.6d9442f65956"));
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "store-logo", "store-desktop", "store-mobile", "room-type-desktop", "room-type-mobile",
                    "independent-site" -> normalized;
            default -> throw new RuntimeException(ApiMessages.get("api.t.65d2d66d422b") + scope);
        };
    }

    private static String safeExtension(String filename, String contentType) {
        if (filename != null) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                String ext = filename.substring(index).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.(jpg|jpeg|png|webp|gif)$")) {
                    return ext;
                }
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
