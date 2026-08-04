package server.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 用服务器已保存的报表字节构造的只读 MultipartFile，供预览/导出复用本月已存文件。
 */
public class StoredSheetMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final byte[] bytes;

    public StoredSheetMultipartFile(String name, String originalFilename, byte[] bytes) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getOriginalFilename() { return originalFilename; }

    @Override
    public String getContentType() { return null; }

    @Override
    public boolean isEmpty() { return bytes.length == 0; }

    @Override
    public long getSize() { return bytes.length; }

    @Override
    public byte[] getBytes() { return bytes.clone(); }

    @Override
    public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), bytes);
    }
}
