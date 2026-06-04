package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.registration.*;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.ReservationRepository;
import server.demo.service.RegistrationAttachmentService;
import server.demo.service.RegistrationAdminService;
import server.demo.service.RegistrationLinkService;
import server.demo.service.RegistrationLinkInboxService;
import server.demo.service.RegistrationMessageService;
import server.demo.service.RegistrationPdfService;
import server.demo.util.StoreContextUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Autowired
    private RegistrationLinkInboxService registrationLinkInboxService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendBaseUrl;

    @GetMapping
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<List<AdminRegistrationListItemDTO>> list(
            @RequestParam(name = "status", required = false) RegistrationFormStatus status,
            @RequestParam(name = "channelId", required = false) Long channelId,
            @RequestParam(name = "reservationStatus", required = false) ReservationStatus reservationStatus,
            @RequestParam(name = "roomNumber", required = false) List<String> roomNumbers,
            @RequestParam(name = "roomNumber[]", required = false) List<String> roomNumberAliases,
            @RequestParam(name = "roomGroupId", required = false) Long roomGroupId,
            @RequestParam(name = "checkInDate", required = false) LocalDate checkInDate,
            @RequestParam(name = "checkOutDate", required = false) LocalDate checkOutDate
    ) {
        return ApiResponse.success(
                "ok",
                registrationAdminService.list(
                        status,
                        channelId,
                        reservationStatus,
                        mergeRoomNumberFilters(roomNumbers, roomNumberAliases),
                        roomGroupId,
                        checkInDate,
                        checkOutDate
                )
        );
    }

    @GetMapping("/link-inbox")
    public ApiResponse<List<RegistrationLinkInboxItemDTO>> linkInbox(
            @RequestParam(name = "reservationStatus", required = false) ReservationStatus reservationStatus
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        return ApiResponse.success("ok", registrationLinkInboxService.listTop200(storeId, reservationStatus));
    }

    @GetMapping("/{formId}")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<AdminRegistrationDetailDTO> detail(@PathVariable Long formId) {
        return ApiResponse.success("ok", registrationAdminService.detail(formId));
    }

    @PostMapping("/{formId}/approve")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<AdminRegistrationReviewResponse> approve(
            @PathVariable Long formId,
            @RequestBody(required = false) AdminRegistrationReviewRequest req
    ) {
        return ApiResponse.success("ok", registrationAdminService.approve(formId, req));
    }

    @PostMapping("/{formId}/reject")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<AdminRegistrationReviewResponse> reject(
            @PathVariable Long formId,
            @RequestBody(required = false) AdminRegistrationReviewRequest req
    ) {
        return ApiResponse.success("ok", registrationAdminService.reject(formId, req));
    }

    @PostMapping("/{formId}/messages/send")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<RegistrationMessageLogDTO> sendMessage(@PathVariable Long formId, @RequestBody RegistrationSendMessageRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        RegistrationMessageLogDTO dto = registrationMessageService.sendMessage(storeId, userId, formId, req);
        return ApiResponse.success("ok", dto);
    }

    @GetMapping("/{formId}/pdf")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
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
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
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
        String base = frontendBaseUrl != null ? frontendBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String link = base + "/r/" + reservation.getOrderNumber() + "?t=" + token;
        return ApiResponse.success("ok", link);
    }

    private static List<String> mergeRoomNumberFilters(List<String> roomNumbers, List<String> roomNumberAliases) {
        List<String> merged = new ArrayList<>();
        addRoomNumberFilters(merged, roomNumbers);
        addRoomNumberFilters(merged, roomNumberAliases);
        if (merged.isEmpty()) {
            return null;
        }
        return merged;
    }

    private static void addRoomNumberFilters(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!target.contains(trimmed)) {
                target.add(trimmed);
            }
        }
    }
}
