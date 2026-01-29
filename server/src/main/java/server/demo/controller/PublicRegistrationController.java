package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ApiResponse;
import server.demo.dto.registration.PublicRegistrationAttachmentDTO;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.dto.registration.PublicRegistrationSaveRequest;
import server.demo.entity.RegistrationAttachment;
import server.demo.service.PublicRegistrationService;
import server.demo.service.RegistrationAttachmentService;
import server.demo.service.RegistrationLinkService;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/public/registration")
public class PublicRegistrationController {

    @Autowired
    private RegistrationLinkService registrationLinkService;

    @Autowired
    private PublicRegistrationService publicRegistrationService;

    @Autowired
    private RegistrationAttachmentService registrationAttachmentService;

    @GetMapping("/{orderNumber}")
    public ApiResponse<PublicRegistrationResponse> get(@PathVariable String orderNumber, @RequestParam(name = "t") String token) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(orderNumber, token);
        PublicRegistrationResponse resp = publicRegistrationService.getOrCreate(claims.getStoreId(), orderNumber);
        return ApiResponse.success("ok", resp);
    }

    @PutMapping("/{orderNumber}")
    public ApiResponse<PublicRegistrationResponse> save(
            @PathVariable String orderNumber,
            @RequestParam(name = "t") String token,
            @RequestBody PublicRegistrationSaveRequest req
    ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(orderNumber, token);
        PublicRegistrationResponse resp = publicRegistrationService.saveDraft(claims.getStoreId(), orderNumber, req);
        return ApiResponse.success("ok", resp);
    }

    @PostMapping("/{orderNumber}/submit")
    public ApiResponse<PublicRegistrationResponse> submit(
            @PathVariable String orderNumber,
            @RequestParam(name = "t") String token
    ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(orderNumber, token);
        PublicRegistrationResponse resp = publicRegistrationService.submit(claims.getStoreId(), orderNumber);
        return ApiResponse.success("ok", resp);
    }

    @PostMapping("/{orderNumber}/attachments/passport")
    public ApiResponse<PublicRegistrationAttachmentDTO> uploadPassport(
            @PathVariable String orderNumber,
            @RequestParam(name = "t") String token,
            @RequestParam(name = "guestId") Long guestId,
            @RequestParam(name = "file") MultipartFile file
    ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(orderNumber, token);
        PublicRegistrationAttachmentDTO dto = registrationAttachmentService.uploadPassport(claims.getStoreId(), orderNumber, guestId, file);
        return ApiResponse.success("ok", dto);
    }

        @GetMapping("/{orderNumber}/attachments/{attachmentId}")
        public ResponseEntity<FileSystemResource> downloadAttachment(
            @PathVariable String orderNumber,
            @RequestParam(name = "t") String token,
            @PathVariable Long attachmentId
        ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(orderNumber, token);
        RegistrationAttachment att = registrationAttachmentService.requireAttachmentForPublicDownload(claims.getStoreId(), orderNumber, attachmentId);
        Path p = registrationAttachmentService.resolveExistingPath(att.getFilePath());

        String contentType = (att.getContentType() == null || att.getContentType().isBlank())
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : att.getContentType();
        String filename = (att.getOriginalName() == null || att.getOriginalName().isBlank())
            ? ("attachment-" + att.getId())
            : att.getOriginalName();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(new FileSystemResource(p));
        }
}
