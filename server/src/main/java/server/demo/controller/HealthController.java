package server.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> getHealthStatus() {
        Map<String, String> healthData = new HashMap<>();
        healthData.put("status", ApiMessages.get("api.t.427c3bd10f14"));
        
        return ApiResponse.success(healthData);
    }
}