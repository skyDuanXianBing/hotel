package server.demo.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ManagedOperationDtos;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.ManagedOperationPdfService;
import server.demo.service.ManagedOperationPrivateStampStorage;
import server.demo.service.ManagedOperationSettingsService;
import server.demo.service.ManagedOperationSettlementService;
import server.demo.util.StoreContextUtils;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/managed-operation-settlement")
@StoreScoped(warmupChannelPrices = false)
public class ManagedOperationSettlementController {
    private final ManagedOperationSettingsService settingsService;
    private final ManagedOperationSettlementService settlementService;
    private final ManagedOperationPdfService pdfService;

    public ManagedOperationSettlementController(
            ManagedOperationSettingsService settingsService,
            ManagedOperationSettlementService settlementService,
            ManagedOperationPdfService pdfService) {
        this.settingsService = settingsService;
        this.settlementService = settlementService;
        this.pdfService = pdfService;
    }

    @GetMapping("/settings")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings(StoreContextUtils.requireStoreId()));
    }

    @PutMapping("/settings")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> saveSettings(
            @RequestBody ManagedOperationDtos.SettingsRequest request) {
        return ApiResponse.success("配置已保存", settingsService.saveSettings(StoreContextUtils.requireStoreId(), request));
    }

    @PostMapping(value = "/stamp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.StampResponse> uploadStamp(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("印章已保存", settingsService.uploadStamp(StoreContextUtils.requireStoreId(), file));
    }

    @GetMapping("/stamp")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<byte[]> getStamp() {
        ManagedOperationPrivateStampStorage.StoredStamp stamp = settingsService.loadStamp(StoreContextUtils.requireStoreId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(stamp.contentType()))
                .body(stamp.bytes());
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.PreviewResponse> preview(
            @RequestPart("airbnbFile") MultipartFile airbnbFile,
            @RequestPart("bookingFile") MultipartFile bookingFile,
            @RequestPart("request") ManagedOperationDtos.RunRequest request) {
        return ApiResponse.success(settlementService.calculate(
                StoreContextUtils.requireStoreId(), airbnbFile, bookingFile, request).preview());
    }

    @PostMapping(value = "/export/{documentType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<byte[]> export(
            @PathVariable String documentType,
            @RequestPart("airbnbFile") MultipartFile airbnbFile,
            @RequestPart("bookingFile") MultipartFile bookingFile,
            @RequestPart("request") ManagedOperationDtos.RunRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        ManagedOperationSettlementService.CalculationResult result = settlementService.calculate(
                storeId, airbnbFile, bookingFile, request);
        ManagedOperationPdfService.ExportFile file = pdfService.export(storeId, documentType, result);
        return buildExportResponse(file);
    }

    static ResponseEntity<byte[]> buildExportResponse(ManagedOperationPdfService.ExportFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.bytes().length)
                .body(file.bytes());
    }
}
