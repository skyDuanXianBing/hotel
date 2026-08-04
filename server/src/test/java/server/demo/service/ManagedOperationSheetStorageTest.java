package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedOperationSheetStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void csv_shouldBeStoredInPrivateStoreScopedPath() {
        byte[] csv = "予約番号,ゲスト\n123,太郎".getBytes();
        ManagedOperationSheetStorage storage = new ManagedOperationSheetStorage(tempDir.toString());

        String key = storage.store(3L, 9L, "airbnb",
                new MockMultipartFile("airbnbFile", "report.csv", "text/csv", csv));

        assertTrue(key.startsWith("3/9/airbnb-"));
        assertTrue(key.endsWith(".csv"));
        Path storedPath = tempDir.resolve("private-managed-operation-sheets").resolve(key);
        assertTrue(Files.isRegularFile(storedPath));
        assertArrayEquals(csv, storage.load(3L, key).bytes());
        assertThrows(ManagedOperationValidationException.class, () -> storage.load(4L, key));

        storage.deleteQuietly(4L, key);
        assertTrue(Files.isRegularFile(storedPath));
        storage.deleteQuietly(3L, key);
        assertFalse(Files.exists(storedPath));
    }

    @Test
    void shouldRejectWrongExtensionEmptyAndOversizeFiles() {
        ManagedOperationSheetStorage storage = new ManagedOperationSheetStorage(tempDir.toString());

        assertThrows(ManagedOperationValidationException.class, () -> storage.store(3L, 9L, "airbnb",
                new MockMultipartFile("airbnbFile", "report.png", "image/png", "x".getBytes())));
        assertThrows(ManagedOperationValidationException.class, () -> storage.store(3L, 9L, "airbnb",
                new MockMultipartFile("airbnbFile", "report.csv", "text/csv", new byte[0])));
        assertThrows(ManagedOperationValidationException.class, () -> storage.store(3L, 9L, "airbnb",
                new MockMultipartFile("airbnbFile", "report.csv", "text/csv", new byte[7 * 1024 * 1024 + 1])));
        assertThrows(ManagedOperationValidationException.class, () -> storage.store(3L, 9L, "unknown",
                new MockMultipartFile("airbnbFile", "report.csv", "text/csv", "x".getBytes())));
    }
}
