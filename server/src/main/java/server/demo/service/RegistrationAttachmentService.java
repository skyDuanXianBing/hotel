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
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;

import server.demo.i18n.ApiMessages;
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
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public PublicRegistrationAttachmentDTO uploadPassport(Long storeId, String orderNumber, Long guestId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException(ApiMessages.get("api.t.c7de0f24a299"));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException(ApiMessages.get("api.t.90aa1bcea605"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new RuntimeException(ApiMessages.get("api.t.284338940052"));
        }

        RegistrationForm form = formRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));

        RegistrationGuest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5a523dd8ceff")));
        if (guest.getForm() == null || !guest.getForm().getId().equals(form.getId())) {
            throw new RuntimeException(ApiMessages.get("api.t.2a1af2703e88"));
        }

        // Only keep one passport image per guest: replace existing ones (best-effort physical cleanup)
        List<RegistrationAttachment> existingPassport = attachmentRepository.findByGuestId(guestId);
        if (existingPassport != null) {
            for (RegistrationAttachment att : existingPassport) {
                if (att.getType() != RegistrationAttachmentType.PASSPORT) {
                    continue;
                }
                try {
                    if (att.getFilePath() != null && !att.getFilePath().isBlank()) {
                        Files.deleteIfExists(Path.of(att.getFilePath()));
                    }
                } catch (Exception ignored) {
                }
                attachmentRepository.delete(att);
            }
        }

        String originalName = file.getOriginalFilename();
        String ext = safeExt(originalName);
        if (!ext.isBlank() && !(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            throw new RuntimeException(ApiMessages.get("api.t.b10c315f34a6"));
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
            throw new RuntimeException(ApiMessages.get("api.t.c4d6ae1edbcb") + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public RegistrationAttachment requireAttachmentForPublicDownload(Long storeId, String orderNumber, Long attachmentId) {
        RegistrationForm form = formRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));

        RegistrationAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.814a0436397f")));

        if (att.getForm() == null || !att.getForm().getId().equals(form.getId())) {
            throw new RuntimeException(ApiMessages.get("api.t.2f9269f36a37"));
        }
        return att;
    }

    @Transactional(readOnly = true)
    public RegistrationAttachment requireAttachmentForAdminDownload(Long storeId, Long formId, Long attachmentId) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.permission.denied"));
        }

        RegistrationAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.814a0436397f")));
        if (att.getForm() == null || !att.getForm().getId().equals(form.getId())) {
            throw new RuntimeException(ApiMessages.get("api.t.2f9269f36a37"));
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
            throw new RuntimeException(ApiMessages.get("api.t.ffcf0a1eb083"));
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
