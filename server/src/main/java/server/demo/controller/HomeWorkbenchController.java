package server.demo.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.home.HomeWorkbenchResponse;
import server.demo.exception.StoreAccessDeniedException;
import server.demo.service.HomeWorkbenchService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/home")
@StoreScoped
public class HomeWorkbenchController {
    private final HomeWorkbenchService homeWorkbenchService;

    public HomeWorkbenchController(HomeWorkbenchService homeWorkbenchService) {
        this.homeWorkbenchService = homeWorkbenchService;
    }

    @GetMapping("/workbench")
    public ResponseEntity<ApiResponse<HomeWorkbenchResponse>> getWorkbench(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Boolean includeSummaries
    ) {
        try {
            HomeWorkbenchResponse response = homeWorkbenchService.getWorkbench(
                    date, limit, type, status, size, cursor, includeSummaries);
            return ResponseEntity.ok(ApiResponse.success("获取首页工作台成功", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (StoreAccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取首页工作台失败: " + e.getMessage()));
        }
    }
}
