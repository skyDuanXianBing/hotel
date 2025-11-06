package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.service.MemoService;

import java.util.Map;

/**
 * 首页备忘录控制器
 */
@RestController
@RequestMapping("/api/v1/memo")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class MemoController {

    @Autowired
    private MemoService memoService;

    /**
     * 获取当前用户的备忘录内容
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> getMemo(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String content = memoService.getMemo(userId);
            return ResponseEntity.ok(ApiResponse.success("获取备忘录成功", content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取备忘录失败: " + e.getMessage()));
        }
    }

    /**
     * 保存当前用户的备忘录内容
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> saveMemo(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String content = request.get("content");

            memoService.saveMemo(userId, content);
            return ResponseEntity.ok(ApiResponse.success("保存备忘录成功", content));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("保存备忘录失败: " + e.getMessage()));
        }
    }
}
