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

/**
 * 媒体文件上传与读取服务。
 */
@Service
public class MediaStorageService {

    private final Path uploadDir;
    private final String serverBaseUrl;

    public MediaStorageService(
            @Value("${media.upload.dir:${user.home}/the-host-hub/uploads/media}") String uploadDir,
            @Value("${server.base-url:http://localhost:8092}") String serverBaseUrl
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.serverBaseUrl = serverBaseUrl != null ? serverBaseUrl.replaceAll("/+$", "") : "";
    }

    public MediaUploadResponseDTO upload(Long storeId, String scope, MultipartFile file) {
        if (storeId == null) {
            throw new RuntimeException("缺少门店上下文");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传文件");
        }
        String normalizedScope = normalizeScope(scope);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("仅支持图片文件");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new RuntimeException("图片大小不能超过 5MB");
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
            response.setUrl(serverBaseUrl + "/media/" + storeId + "/" + normalizedScope + "/" + filename);
            response.setOriginalName(originalName);
            response.setContentType(contentType);
            response.setFileSize(file.getSize());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
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
            throw new RuntimeException("文件不存在");
        }
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new RuntimeException("scope 不能为空");
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "store-logo", "store-desktop", "store-mobile", "room-type-desktop", "room-type-mobile" -> normalized;
            default -> throw new RuntimeException("不支持的上传类型: " + scope);
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
