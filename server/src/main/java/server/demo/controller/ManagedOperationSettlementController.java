package server.demo.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ManagedOperationDtos;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.ManagedOperationMonthlyDataService;
import server.demo.service.ManagedOperationPdfService;
import server.demo.service.ManagedOperationPrivateStampStorage;
import server.demo.service.ManagedOperationSettingsService;
import server.demo.service.ManagedOperationSettlementService;
import server.demo.util.StoreContextUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/managed-operation-settlement")
@StoreScoped(warmupChannelPrices = false)
public class ManagedOperationSettlementController {
    private final ManagedOperationSettingsService settingsService;
    private final ManagedOperationSettlementService settlementService;
    private final ManagedOperationMonthlyDataService monthlyDataService;
    private final ManagedOperationPdfService pdfService;

    public ManagedOperationSettlementController(
            ManagedOperationSettingsService settingsService,
            ManagedOperationSettlementService settlementService,
            ManagedOperationMonthlyDataService monthlyDataService,
            ManagedOperationPdfService pdfService) {
        this.settingsService = settingsService;
        this.settlementService = settlementService;
        this.monthlyDataService = monthlyDataService;
        this.pdfService = pdfService;
    }

    @GetMapping("/properties")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<List<ManagedOperationDtos.PropertySummary>> listProperties() {
        return ApiResponse.success(settingsService.listProperties(StoreContextUtils.requireStoreId()));
    }

    @PostMapping("/properties")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> createProperty(
            @RequestBody ManagedOperationDtos.CreatePropertyRequest request) {
        return ApiResponse.success(ApiMessages.get("api.t.6fc3f91447ed"),
                settingsService.createProperty(StoreContextUtils.requireStoreId(), request));
    }

    @GetMapping("/properties/{settingsId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> getSettings(@PathVariable Long settingsId) {
        return ApiResponse.success(settingsService.getSettings(StoreContextUtils.requireStoreId(), settingsId));
    }

    @PutMapping("/properties/{settingsId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> saveSettings(
            @PathVariable Long settingsId,
            @RequestBody ManagedOperationDtos.SettingsRequest request) {
        return ApiResponse.success(ApiMessages.get("api.t.c4e4c1a74293"),
                settingsService.saveSettings(StoreContextUtils.requireStoreId(), settingsId, request));
    }

    @PatchMapping("/properties/{settingsId}/issue-day")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.SettingsResponse> updateIssueDay(
            @PathVariable Long settingsId,
            @RequestBody ManagedOperationDtos.IssueDayRequest request) {
        return ApiResponse.success(ApiMessages.get("api.t.c4e4c1a74293"),
                settingsService.updateIssueDay(StoreContextUtils.requireStoreId(), settingsId, request));
    }

    @DeleteMapping("/properties/{settingsId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<Void> deleteProperty(@PathVariable Long settingsId) {
        settingsService.deleteProperty(StoreContextUtils.requireStoreId(), settingsId);
        return ApiResponse.success(ApiMessages.get("api.t.48dc0f0dde7e"), null);
    }

    @PostMapping(value = "/properties/{settingsId}/stamp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.StampResponse> uploadStamp(
            @PathVariable Long settingsId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(ApiMessages.get("api.t.fcd62c76e017"),
                settingsService.uploadStamp(StoreContextUtils.requireStoreId(), settingsId, file));
    }

    @GetMapping("/properties/{settingsId}/stamp")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<byte[]> getStamp(@PathVariable Long settingsId) {
        ManagedOperationPrivateStampStorage.StoredStamp stamp = settingsService.loadStamp(
                StoreContextUtils.requireStoreId(), settingsId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(stamp.contentType()))
                .body(stamp.bytes());
    }

    @GetMapping("/properties/{settingsId}/monthly")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.MonthlyDataResponse> getMonthlyData(
            @PathVariable Long settingsId,
            @RequestParam("month") String month) {
        return ApiResponse.success(monthlyDataService.getMonthlyData(
                StoreContextUtils.requireStoreId(), settingsId, month));
    }

    @PostMapping(value = "/properties/{settingsId}/monthly", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.MonthlyDataResponse> saveMonthlyData(
            @PathVariable Long settingsId,
            @RequestPart(value = "airbnbFile", required = false) MultipartFile airbnbFile,
            @RequestPart(value = "bookingFile", required = false) MultipartFile bookingFile,
            @RequestPart("request") ManagedOperationDtos.MonthlyDataRequest request) {
        return ApiResponse.success(ApiMessages.get("api.t.42beb9a360fb"),
                monthlyDataService.saveMonthlyData(
                        StoreContextUtils.requireStoreId(), settingsId, request, airbnbFile, bookingFile));
    }

    @GetMapping("/properties/{settingsId}/document-numbers")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.DocumentNumberSuggestion> suggestDocumentNumbers(
            @PathVariable Long settingsId,
            @RequestParam("month") String month) {
        return ApiResponse.success(monthlyDataService.suggestDocumentNumbers(
                StoreContextUtils.requireStoreId(), settingsId, month));
    }

    @PostMapping(value = "/properties/{settingsId}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ApiResponse<ManagedOperationDtos.PreviewResponse> preview(
            @PathVariable Long settingsId,
            @RequestPart(value = "airbnbFile", required = false) MultipartFile airbnbFile,
            @RequestPart(value = "bookingFile", required = false) MultipartFile bookingFile,
            @RequestPart("request") ManagedOperationDtos.RunRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        ManagedOperationMonthlyDataService.ResolvedSheets sheets = monthlyDataService.resolveSheets(
                storeId, settingsId, request == null ? null : request.settlementMonth(),
                airbnbFile, bookingFile);
        return ApiResponse.success(settlementService.calculate(
                storeId, settingsId, sheets.airbnbFile(), sheets.bookingFile(), request).preview());
    }

    @PostMapping(value = "/properties/{settingsId}/export/{documentType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<byte[]> export(
            @PathVariable Long settingsId,
            @PathVariable String documentType,
            @RequestPart(value = "airbnbFile", required = false) MultipartFile airbnbFile,
            @RequestPart(value = "bookingFile", required = false) MultipartFile bookingFile,
            @RequestPart("request") ManagedOperationDtos.RunRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        ManagedOperationMonthlyDataService.ResolvedSheets sheets = monthlyDataService.resolveSheets(
                storeId, settingsId, request == null ? null : request.settlementMonth(),
                airbnbFile, bookingFile);
        ManagedOperationSettlementService.CalculationResult result = settlementService.calculate(
                storeId, settingsId, sheets.airbnbFile(), sheets.bookingFile(), request);
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
