package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.AnnouncementDTO;
import server.demo.dto.ApiResponse;
import server.demo.service.AnnouncementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/announcements")
@StoreScoped
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<List<AnnouncementDTO>>> listHomeAnnouncements(
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            List<AnnouncementDTO> announcements = announcementService.listHomeAnnouncements(locale, limit);
            return ResponseEntity.ok(ApiResponse.success("获取首页公告成功", announcements));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取首页公告失败: " + e.getMessage()));
        }
    }
}
