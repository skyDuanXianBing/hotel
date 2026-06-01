package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.RoomStatusShareRequest;
import server.demo.dto.RoomStatusShareResponse;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.entity.RoomStatusShare;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.RoomStatusShareService;
import server.demo.service.RoomStatusService;
import server.demo.util.StoreTimeZoneUtil;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/room-status-share")
public class RoomStatusShareController {

    @Autowired
    private RoomStatusShareService roomStatusShareService;

    @Autowired
    private RoomStatusService roomStatusService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private Clock clock;

    /**
     * 获取分享列表
     */
    @GetMapping
    public ApiResponse<RoomStatusShareResponse> getShares(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        
        try {
            RoomStatusShareResponse response = roomStatusShareService.getShares(page, pageSize);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取分享列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建分享链接
     */
    @PostMapping
    public ApiResponse<RoomStatusShare> createShare(@Valid @RequestBody RoomStatusShareRequest request) {
        try {
            RoomStatusShare share = roomStatusShareService.createShare(request);
            return ApiResponse.success(share);
        } catch (Exception e) {
            return ApiResponse.error("创建分享链接失败: " + e.getMessage());
        }
    }

    /**
     * 更新分享链接
     */
    @PutMapping("/{id}")
    public ApiResponse<RoomStatusShare> updateShare(
            @PathVariable Long id,
            @Valid @RequestBody RoomStatusShareRequest request) {
        try {
            RoomStatusShare share = roomStatusShareService.updateShare(id, request);
            return ApiResponse.success(share);
        } catch (Exception e) {
            return ApiResponse.error("更新分享链接失败: " + e.getMessage());
        }
    }

    /**
     * 删除分享链接
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteShare(@PathVariable Long id) {
        try {
            roomStatusShareService.deleteShare(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            return ApiResponse.error("删除分享链接失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享详情（用于分享页面展示）
     */
    @GetMapping("/public/{shareToken}")
    public ApiResponse<RoomStatusShare> getShareByToken(@PathVariable String shareToken) {
        try {
            RoomStatusShare share = roomStatusShareService.getShareByToken(shareToken);
            return ApiResponse.success(share);
        } catch (Exception e) {
            return ApiResponse.error("获取分享信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享页面的房态数据
     */
    @GetMapping("/public/{shareToken}/room-status")
    public ApiResponse<RoomStatusCalendarDTO> getSharedRoomStatus(
            @PathVariable String shareToken,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            // 验证分享链接有效性
            RoomStatusShare share = roomStatusShareService.getShareByToken(shareToken);

            Long userId = share.getUserId();
            if (userId == null) {
                return ApiResponse.error("无法获取分享用户信息");
            }
            Long storeId = share.getStoreId();
            if (storeId == null) {
                return ApiResponse.error("无法获取分享门店信息");
            }

            // 设置默认日期范围（从分享门店今天开始7天）
            if (startDate == null) {
                startDate = storeToday(storeId);
            }
            if (endDate == null) {
                endDate = startDate.plusDays(6);
            }

            // 获取房态数据 - 使用分享链接关联的用户ID
            RoomStatusCalendarDTO roomStatusData = roomStatusService.getRoomStatusCalendarForStore(storeId, startDate, endDate);
            
            // 根据分享配置过滤房间数据
            RoomStatusCalendarDTO filteredData = roomStatusShareService.filterRoomStatusByShare(
                share, roomStatusData);
            
            return ApiResponse.success(filteredData);
        } catch (Exception e) {
            return ApiResponse.error("获取房态数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享页面的统计数据
     */
    @GetMapping("/public/{shareToken}/statistics")
    public ApiResponse<RoomStatusStatisticsDTO> getSharedStatistics(
            @PathVariable String shareToken,
            @RequestParam(required = false) LocalDate date) {
        try {
            // 验证分享链接有效性
            RoomStatusShare share = roomStatusShareService.getShareByToken(shareToken);

            // 获取分享用户的ID
            Long storeId = share.getStoreId();
            if (storeId == null) {
                return ApiResponse.error("无法获取分享门店信息");
            }

            // 设置默认日期为分享门店今天
            if (date == null) {
                date = storeToday(storeId);
            }

            RoomStatusStatisticsDTO statistics = roomStatusService.getRoomStatusStatisticsForStore(storeId, date);

            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        }
    }

    private LocalDate storeToday(Long storeId) {
        ZoneId zoneId = resolveStoreZoneId(storeId);
        return LocalDate.now(effectiveClock().withZone(zoneId));
    }

    private ZoneId resolveStoreZoneId(Long storeId) {
        Store store = storeId == null || storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private Clock effectiveClock() {
        return clock != null ? clock : Clock.systemUTC();
    }
}
