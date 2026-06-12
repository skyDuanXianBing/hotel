package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.AnnouncementDTO;
import server.demo.dto.ApiResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
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

    @GetMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.VIEW_SETTINGS)
    public ResponseEntity<ApiResponse<List<AnnouncementDTO>>> listStoreAnnouncements() {
        try {
            List<AnnouncementDTO> announcements = announcementService.listStoreAnnouncements();
            return ResponseEntity.ok(ApiResponse.success("获取门店公告成功", announcements));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取门店公告失败: " + e.getMessage()));
        }
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

    @PostMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<AnnouncementDTO>> createStoreAnnouncement(
            @RequestBody AnnouncementDTO request
    ) {
        try {
            AnnouncementDTO announcement = announcementService.createStoreAnnouncement(request);
            return ResponseEntity.ok(ApiResponse.success("创建门店公告成功", announcement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("创建门店公告失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<AnnouncementDTO>> updateStoreAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementDTO request
    ) {
        try {
            AnnouncementDTO announcement = announcementService.updateStoreAnnouncement(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新门店公告成功", announcement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("更新门店公告失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<AnnouncementDTO>> disableStoreAnnouncement(
            @PathVariable Long id
    ) {
        try {
            AnnouncementDTO announcement = announcementService.disableStoreAnnouncement(id);
            return ResponseEntity.ok(ApiResponse.success("停用门店公告成功", announcement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("停用门店公告失败: " + e.getMessage()));
        }
    }
}
