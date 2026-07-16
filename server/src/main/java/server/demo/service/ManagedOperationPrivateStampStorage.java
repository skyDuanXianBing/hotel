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
            throw new ManagedOperationValidationException("请上传印章图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ManagedOperationValidationException("印章图片不能超过 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = detectExtension(bytes);
            validateDecodedImage(bytes);
            String filename = UUID.randomUUID() + extension;
            Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
            Path target = storeDir.resolve(filename).normalize();
            if (!target.startsWith(storeDir)) {
                throw new ManagedOperationValidationException("非法印章存储路径");
            }
            Files.createDirectories(storeDir);
            Files.write(target, bytes);
            return storeId + "/" + filename;
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ManagedOperationValidationException("印章图片保存失败", ex);
        }
    }

    public StoredStamp load(Long storeId, String storageKey) {
        if (storeId == null || storageKey == null || storageKey.isBlank()
                || !storageKey.startsWith(storeId + "/")) {
            throw new ManagedOperationValidationException("印章不存在");
        }
        Path path = privateRoot.resolve(storageKey).normalize();
        Path storeDir = privateRoot.resolve(String.valueOf(storeId)).normalize();
        if (!path.startsWith(storeDir) || !Files.isRegularFile(path)) {
            throw new ManagedOperationValidationException("印章不存在");
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String extension = detectExtension(bytes);
            return new StoredStamp(bytes, ".png".equals(extension) ? "image/png" : "image/jpeg");
        } catch (IOException ex) {
            throw new ManagedOperationValidationException("印章读取失败", ex);
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
        throw new ManagedOperationValidationException("印章仅支持真实的 PNG 或 JPEG 图片");
    }

    private static void validateDecodedImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new ManagedOperationValidationException("印章图片无法解码");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ManagedOperationValidationException("印章图片无法解码");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new ManagedOperationValidationException("印章图片尺寸不能超过 4096×4096");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ManagedOperationValidationException("印章图片无法解码");
            } finally {
                reader.dispose();
            }
        }
    }

    public record StoredStamp(byte[] bytes, String contentType) {}
}
