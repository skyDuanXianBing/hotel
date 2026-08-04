package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import server.demo.i18n.ApiMessages;

/**
 * 代运营月度报表（Airbnb / Booking CSV、XLS、XLSX）的私有磁盘存储。
 * 仅通过门店隔离的相对 key 访问，任何路径都不暴露给外部。
 */
@Service
public class ManagedOperationSheetStorage {
    private static final long MAX_BYTES = 7L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xls", "xlsx");
    private final Path privateRoot;

    public ManagedOperationSheetStorage(
            @Value("${media.upload.dir:${user.home}/the-host-hub/uploads/media}") String uploadDir) {
        this.privateRoot = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve("private-managed-operation-sheets").normalize();
    }

    public String store(Long storeId, Long settingsId, String kind, MultipartFile file) {
        if (storeId == null || settingsId == null || file == null || file.isEmpty()) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.565540b5ff3e"));
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.6bcea87c4803"));
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.301ddd4acf3a"));
        }
        if (!"airbnb".equals(kind) && !"booking".equals(kind)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.2720bbee5cc2"));
        }
        try {
            byte[] bytes = file.getBytes();
            String filename = kind + "-" + UUID.randomUUID() + "." + extension;
            Path settingsDir = privateRoot.resolve(String.valueOf(storeId))
                    .resolve(String.valueOf(settingsId)).normalize();
            Path target = settingsDir.resolve(filename).normalize();
            if (!target.startsWith(settingsDir)) {
                throw new ManagedOperationValidationException(ApiMessages.get("api.t.2720bbee5cc2"));
            }
            Files.createDirectories(settingsDir);
            Files.write(target, bytes);
            return storeId + "/" + settingsId + "/" + filename;
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.e42b19acc8f8"), ex);
        }
    }

    public StoredSheet load(Long storeId, String storageKey) {
        if (storeId == null || storageKey == null || storageKey.isBlank()
                || !storageKey.startsWith(storeId + "/")) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.447f537d462d"));
        }
        Path path = privateRoot.resolve(storageKey).normalize();
        Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
        if (!path.startsWith(storeDir) || !Files.isRegularFile(path)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.447f537d462d"));
        }
        try {
            return new StoredSheet(Files.readAllBytes(path));
        } catch (IOException ex) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.7ad9a6bb0f6f"), ex);
        }
    }

    public void deleteQuietly(Long storeId, String storageKey) {
        if (storeId == null || storageKey == null || !storageKey.startsWith(storeId + "/")) {
            return;
        }
        try {
            Path path = privateRoot.resolve(storageKey).normalize();
            Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
            if (path.startsWith(storeDir)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Orphan cleanup can be performed separately; never fail the saved configuration.
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredSheet(byte[] bytes) {}
}
