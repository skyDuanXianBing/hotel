package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.*;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.BusinessStatisticsService;

import java.time.LocalDate;
import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/statistics/business")
@StoreScoped
public class BusinessStatisticsController {

    @Autowired
    private BusinessStatisticsService businessStatisticsService;

    /**
     * 获取营业汇总统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 营业汇总数据
     */
    @GetMapping("/summary")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<BusinessSummaryDTO> getBusinessSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            BusinessSummaryDTO summary = businessStatisticsService.getBusinessSummary(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.d2829f11cc88"), summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.f9230ddda680") + e.getMessage());
        }
    }

    /**
     * 获取每日入住率统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日入住率数据
     */
    @GetMapping("/daily-occupancy")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<List<DailyOccupancyDTO>> getDailyOccupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            List<DailyOccupancyDTO> occupancyList = businessStatisticsService.getDailyOccupancy(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.7dbc0836a7cc"), occupancyList);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.799508da3449") + e.getMessage());
        }
    }

    /**
     * 获取营业概况详细统计（数据中心-营业概况标签页使用）
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 营业概况详细数据
     */
    @GetMapping("/overview")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<BusinessOverviewDTO> getBusinessOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            BusinessOverviewDTO overview = businessStatisticsService.getBusinessOverview(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.f3f3caccea46"), overview);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.65942cd490d2") + e.getMessage());
        }
    }

    /**
     * 获取流水汇总统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 流水汇总数据
     */
    @GetMapping("/revenue-summary")
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.VIEW_FINANCIAL_DATA)
    public ApiResponse<RevenueSummaryDTO> getRevenueSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            RevenueSummaryDTO summary = businessStatisticsService.getRevenueSummary(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.c8815cc66065"), summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.dce08a71201e") + e.getMessage());
        }
    }

    /**
     * 获取渠道汇总统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 渠道汇总数据
     */
    @GetMapping("/channel-summary")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<ChannelSummaryDTO> getChannelSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            ChannelSummaryDTO summary = businessStatisticsService.getChannelSummary(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.213137f8e90a"), summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.6c62aec9745c") + e.getMessage());
        }
    }

    /**
     * 获取销售汇总统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param keyword 搜索关键词(可选)
     * @param channelId 渠道ID(可选)
     * @return 销售汇总数据
     */
    @GetMapping("/sales-summary")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<SalesSummaryDTO> getSalesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            SalesSummaryDTO summary = businessStatisticsService.getSalesSummary(
                    startDate,
                    endDate,
                    keyword,
                    channelId,
                    customer,
                    page,
                    pageSize
            );
            return ApiResponse.success(ApiMessages.get("api.t.1791c9c317bf"), summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.335e955d3a27") + e.getMessage());
        }
    }

    /**
     * 获取经营指标统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 经营指标数据
     */
    @GetMapping("/operational-metrics")
    @RequirePermission(module = PermissionModule.STATISTICS, action = PermissionAction.VIEW_STATS)
    public ApiResponse<OperationalMetricsDTO> getOperationalMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error(ApiMessages.get("api.t.43318cbb9f3c"));
            }

            OperationalMetricsDTO metrics = businessStatisticsService.getOperationalMetrics(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.7687b19c35c1"), metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.2f09485e6105") + e.getMessage());
        }
    }
}
