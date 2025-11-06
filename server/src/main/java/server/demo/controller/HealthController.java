package server.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> getHealthStatus() {
        Map<String, String> healthData = new HashMap<>();
        healthData.put("status", "健康运行中");
        
        return ApiResponse.success(healthData);
    }
}