package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.service.MemoService;

import java.util.Map;

/**
 * 首页备忘录控制器（门店级架构）
 */
@RestController
@RequestMapping("/api/v1/memo")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
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
            return ResponseEntity.ok(ApiResponse.success("获取备忘录成功", content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取备忘录失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("保存备忘录成功", content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("保存备忘录失败: " + e.getMessage()));
        }
    }
}
