package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.registration.PublicRegistrationAttachmentDTO;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.enums.RegistrationAttachmentType;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RegistrationAttachmentService {

    private final RegistrationAttachmentRepository attachmentRepository;
    private final RegistrationFormRepository formRepository;
    private final RegistrationGuestRepository guestRepository;
    private final Path uploadDir;

    public RegistrationAttachmentService(
            RegistrationAttachmentRepository attachmentRepository,
            RegistrationFormRepository formRepository,
            RegistrationGuestRepository guestRepository,
            @Value("${registration.upload.dir:uploads/registration}") String uploadDir
    ) {
        this.attachmentRepository = attachmentRepository;
        this.formRepository = formRepository;
        this.guestRepository = guestRepository;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional
    public PublicRegistrationAttachmentDTO uploadPassport(Long storeId, String orderNumber, Long guestId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传文件");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("文件过大(最大10MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new RuntimeException("仅支持图片文件");
        }

        RegistrationForm form = formRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));

        RegistrationGuest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("人员不存在"));
        if (guest.getForm() == null || !guest.getForm().getId().equals(form.getId())) {
            throw new RuntimeException("人员不属于该登记表");
        }

        String originalName = file.getOriginalFilename();
        String ext = safeExt(originalName);
        if (!ext.isBlank() && !(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            throw new RuntimeException("仅支持 jpg/jpeg/png/webp");
        }
        String filename = UUID.randomUUID() + (ext.isBlank() ? "" : ("." + ext));

        Path dir = uploadDir.resolve(String.valueOf(storeId)).resolve(orderNumber);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    sha256.update(buf, 0, n);
                }
            }

            file.transferTo(target.toFile());

            RegistrationAttachment att = new RegistrationAttachment();
            att.setForm(form);
            att.setGuest(guest);
            att.setType(RegistrationAttachmentType.PASSPORT);
            att.setFilePath(target.toString());
            att.setOriginalName(originalName);
            att.setContentType(contentType);
            att.setFileSize(file.getSize());
            att.setSha256(HexFormat.of().formatHex(sha256.digest()));
            att = attachmentRepository.save(att);

            PublicRegistrationAttachmentDTO dto = new PublicRegistrationAttachmentDTO();
            dto.setId(att.getId());
            dto.setGuestId(guest.getId());
            dto.setType(att.getType());
            dto.setOriginalName(att.getOriginalName());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public RegistrationAttachment requireAttachmentForPublicDownload(Long storeId, String orderNumber, Long attachmentId) {
        RegistrationForm form = formRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));

        RegistrationAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("附件不存在"));

        if (att.getForm() == null || !att.getForm().getId().equals(form.getId())) {
            throw new RuntimeException("附件不属于该登记表");
        }
        return att;
    }

    @Transactional(readOnly = true)
    public RegistrationAttachment requireAttachmentForAdminDownload(Long storeId, Long formId, Long attachmentId) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        RegistrationAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("附件不存在"));
        if (att.getForm() == null || !att.getForm().getId().equals(form.getId())) {
            throw new RuntimeException("附件不属于该登记表");
        }
        return att;
    }

    public Path resolveExistingPath(String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                throw new NoSuchFileException("empty");
            }
            Path p = Path.of(filePath);
            if (!Files.exists(p)) {
                throw new NoSuchFileException(p.toString());
            }
            return p;
        } catch (Exception e) {
            throw new RuntimeException("文件不存在");
        }
    }

    private String safeExt(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(idx + 1).toLowerCase();
        if (ext.length() > 10) {
            return "";
        }
        return ext.replaceAll("[^a-z0-9]", "");
    }
}
