package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.PriceChangeHistoryPageResponse;
import server.demo.service.PriceChangeHistoryService;

import java.time.LocalDate;

/**
 * 改价历史Controller
 */
@RestController
@RequestMapping("/api/v1/price-change-history")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class PriceChangeHistoryController {

    @Autowired
    private PriceChangeHistoryService priceChangeHistoryService;

    /**
     * 分页查询改价历史
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PriceChangeHistoryPageResponse>> getPriceChangeHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operateDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operateDateEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate priceDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate priceDateEnd,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Long pricePlanId,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "25") Integer pageSize
    ) {
        try {
            PriceChangeHistoryPageResponse response = priceChangeHistoryService.getPriceChangeHistory(
                    operateDateStart,
                    operateDateEnd,
                    priceDateStart,
                    priceDateEnd,
                    roomTypeId,
                    pricePlanId,
                    operator,
                    pageNum,
                    pageSize
            );
            return ResponseEntity.ok(ApiResponse.success("获取改价历史成功", response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取改价历史失败: " + e.getMessage()));
        }
    }
}
