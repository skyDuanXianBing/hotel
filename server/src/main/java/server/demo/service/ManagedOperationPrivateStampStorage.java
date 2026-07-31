package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

import server.demo.i18n.ApiMessages;
@Service
public class ManagedOperationPrivateStampStorage {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private final Path privateRoot;

    public ManagedOperationPrivateStampStorage(
            @Value("${media.upload.dir:${user.home}/the-host-hub/uploads/media}") String uploadDir) {
        this.privateRoot = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve("private-managed-operation-stamps").normalize();
    }

    public String store(Long storeId, MultipartFile file) {
        if (storeId == null || file == null || file.isEmpty()) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.13901c3cb687"));
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.b3f763475214"));
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = detectExtension(bytes);
            validateDecodedImage(bytes);
            String filename = UUID.randomUUID() + extension;
            Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
            Path target = storeDir.resolve(filename).normalize();
            if (!target.startsWith(storeDir)) {
                throw new ManagedOperationValidationException(ApiMessages.get("api.t.2720bbee5cc2"));
            }
            Files.createDirectories(storeDir);
            Files.write(target, bytes);
            return storeId + "/" + filename;
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.bce8a372025f"), ex);
        }
    }

    public StoredStamp load(Long storeId, String storageKey) {
        if (storeId == null || storageKey == null || storageKey.isBlank()
                || !storageKey.startsWith(storeId + "/")) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.22a21e210618"));
        }
        Path path = privateRoot.resolve(storageKey).normalize();
        Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
        if (!path.startsWith(storeDir) || !Files.isRegularFile(path)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.22a21e210618"));
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String extension = detectExtension(bytes);
            return new StoredStamp(bytes, ".png".equals(extension) ? "image/png" : "image/jpeg");
        } catch (IOException ex) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.4b277b36aed9"), ex);
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

    private static String detectExtension(byte[] bytes) {
        if (bytes != null && bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return ".png";
        }
        if (bytes != null && bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        throw new ManagedOperationValidationException(ApiMessages.get("api.t.c418b7730b98"));
    }

    private static void validateDecodedImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new ManagedOperationValidationException(ApiMessages.get("api.t.c9153d01477c"));
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ManagedOperationValidationException(ApiMessages.get("api.t.c9153d01477c"));
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new ManagedOperationValidationException(ApiMessages.get("api.t.786141e60f36"));
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ManagedOperationValidationException(ApiMessages.get("api.t.c9153d01477c"));
            } finally {
                reader.dispose();
            }
        }
    }

    public record StoredStamp(byte[] bytes, String contentType) {}
}
