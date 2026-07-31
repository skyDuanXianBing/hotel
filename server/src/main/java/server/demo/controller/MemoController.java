package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.service.MemoService;

import java.util.Map;

import server.demo.i18n.ApiMessages;
/**
 * 首页备忘录控制器（门店级架构）
 */
@RestController
@RequestMapping("/api/v1/memo")
@StoreScoped
public class MemoController {

    @Autowired
    private MemoService memoService;

    /**
     * 获取当前门店的备忘录内容
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> getMemo() {
        try {
            String content = memoService.getMemo();
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e20464abb4eb"), content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.5328b27a2c51") + e.getMessage()));
        }
    }

    /**
     * 保存当前门店的备忘录内容
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> saveMemo(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            memoService.saveMemo(content);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.174559f80525"), content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.fbe02ab81e2e") + e.getMessage()));
        }
    }
}
