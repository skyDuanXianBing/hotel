package server.demo.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.StatisticsReportExportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/statistics/reports")
@StoreScoped
public class StatisticsReportController {

    private final StatisticsReportExportService reportExportService;

    public StatisticsReportController(StatisticsReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @GetMapping("/{type}")
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.VIEW_FINANCIAL_DATA)
    public ResponseEntity<?> exportReport(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String customer
    ) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("开始日期不能晚于结束日期"));
        }
        if (!reportExportService.isSupported(type)) {
            String message = "不支持的报表类型: " + type
                    + "；当前支持: " + String.join(", ", reportExportService.supportedTypes());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error(message));
        }

        String csv = reportExportService.exportCsv(type, startDate, endDate, keyword, channelId, customer);
        String fileName = type + "-" + startDate + "-" + endDate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
