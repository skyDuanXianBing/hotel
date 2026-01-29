package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.registration.*;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.ReservationRepository;
import server.demo.service.RegistrationAttachmentService;
import server.demo.service.RegistrationAdminService;
import server.demo.service.RegistrationLinkService;
import server.demo.service.RegistrationMessageService;
import server.demo.service.RegistrationPdfService;
import server.demo.util.StoreContextUtils;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
@StoreScoped
public class RegistrationAdminController {

    @Autowired
    private RegistrationAdminService registrationAdminService;

    @Autowired
    private RegistrationFormRepository registrationFormRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RegistrationGuestRepository registrationGuestRepository;

    @Autowired
    private RegistrationPdfService registrationPdfService;

    @Autowired
    private RegistrationLinkService registrationLinkService;

    @Autowired
    private RegistrationAttachmentService registrationAttachmentService;

    @Autowired
    private RegistrationMessageService registrationMessageService;

    @org.springframework.beans.factory.annotation.Value("${server.base-url}")
    private String serverBaseUrl;

    @GetMapping
    public ApiResponse<List<AdminRegistrationListItemDTO>> list(@RequestParam(name = "status", required = false) RegistrationFormStatus status) {
        return ApiResponse.success("ok", registrationAdminService.list(status));
    }

    @GetMapping("/{formId}")
    public ApiResponse<AdminRegistrationDetailDTO> detail(@PathVariable Long formId) {
        return ApiResponse.success("ok", registrationAdminService.detail(formId));
    }

    @PostMapping("/{formId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long formId, @RequestBody(required = false) AdminRegistrationReviewRequest req) {
        registrationAdminService.approve(formId, req);
        return ApiResponse.success("ok", null);
    }

    @PostMapping("/{formId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long formId, @RequestBody(required = false) AdminRegistrationReviewRequest req) {
        registrationAdminService.reject(formId, req);
        return ApiResponse.success("ok", null);
    }

    @PostMapping("/{formId}/messages/send")
    public ApiResponse<RegistrationMessageLogDTO> sendMessage(@PathVariable Long formId, @RequestBody RegistrationSendMessageRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        RegistrationMessageLogDTO dto = registrationMessageService.sendMessage(storeId, userId, formId, req);
        return ApiResponse.success("ok", dto);
    }

    @GetMapping("/{formId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long formId) {
        Long storeId = StoreContextUtils.requireStoreId();
        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        Reservation reservation = reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        List<RegistrationGuest> guests = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());

        byte[] pdf = registrationPdfService.render(form, reservation, guests);
        String filename = "registration-" + form.getOrderNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

        @GetMapping("/{formId}/attachments/{attachmentId}")
        public ResponseEntity<FileSystemResource> downloadAttachment(@PathVariable Long formId, @PathVariable Long attachmentId) {
        Long storeId = StoreContextUtils.requireStoreId();
        RegistrationAttachment att = registrationAttachmentService.requireAttachmentForAdminDownload(storeId, formId, attachmentId);
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

    @GetMapping("/link/{orderNumber}")
    public ApiResponse<String> generateLink(@PathVariable String orderNumber) {
        Long storeId = StoreContextUtils.requireStoreId();
        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        String token = registrationLinkService.generateToken(storeId, orderNumber);
        String base = serverBaseUrl != null ? serverBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String link = base + "/r/" + reservation.getOrderNumber() + "?t=" + token;
        return ApiResponse.success("ok", link);
    }
}
