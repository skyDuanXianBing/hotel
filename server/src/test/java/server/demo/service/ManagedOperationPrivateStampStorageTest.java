package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedOperationPrivateStampStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void realPng_shouldBeStoredInPrivateStoreScopedPath() throws Exception {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        ManagedOperationPrivateStampStorage storage = new ManagedOperationPrivateStampStorage(tempDir.toString());

        String key = storage.store(3L, new MockMultipartFile("file", "stamp.png", "image/png", png));

        assertTrue(key.startsWith("3/"));
        assertFalse(key.contains("/media/"));
        Path storedPath = tempDir.resolve("private-managed-operation-stamps").resolve(key);
        assertTrue(Files.isRegularFile(storedPath));
        assertArrayEquals(png, storage.load(3L, key).bytes());
        assertThrows(ManagedOperationValidationException.class, () -> storage.load(4L, key));

        storage.deleteQuietly(4L, key);
        assertTrue(Files.isRegularFile(storedPath));
        storage.deleteQuietly(3L, key);
        assertFalse(Files.exists(storedPath));
    }

    @Test
    void extensionOrContentTypeCannotDisguiseNonImage() {
        ManagedOperationPrivateStampStorage storage = new ManagedOperationPrivateStampStorage(tempDir.toString());
        assertThrows(ManagedOperationValidationException.class, () -> storage.store(3L,
                new MockMultipartFile("file", "stamp.png", "image/png", "<svg/>".getBytes())));
    }
}
