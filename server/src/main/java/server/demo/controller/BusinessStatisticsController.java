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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            BusinessSummaryDTO summary = businessStatisticsService.getBusinessSummary(startDate, endDate);
            return ApiResponse.success("获取营业汇总成功", summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取营业汇总失败: " + e.getMessage());
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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            List<DailyOccupancyDTO> occupancyList = businessStatisticsService.getDailyOccupancy(startDate, endDate);
            return ApiResponse.success("获取每日入住率成功", occupancyList);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取每日入住率失败: " + e.getMessage());
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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            BusinessOverviewDTO overview = businessStatisticsService.getBusinessOverview(startDate, endDate);
            return ApiResponse.success("获取营业概况成功", overview);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取营业概况失败: " + e.getMessage());
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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            RevenueSummaryDTO summary = businessStatisticsService.getRevenueSummary(startDate, endDate);
            return ApiResponse.success("获取流水汇总成功", summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取流水汇总失败: " + e.getMessage());
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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            ChannelSummaryDTO summary = businessStatisticsService.getChannelSummary(startDate, endDate);
            return ApiResponse.success("获取渠道汇总成功", summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取渠道汇总失败: " + e.getMessage());
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
            @RequestParam(required = false) Long channelId) {

        try {
            if (startDate.isAfter(endDate)) {
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            SalesSummaryDTO summary = businessStatisticsService.getSalesSummary(startDate, endDate, keyword, channelId);
            return ApiResponse.success("获取销售汇总成功", summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取销售汇总失败: " + e.getMessage());
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
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            OperationalMetricsDTO metrics = businessStatisticsService.getOperationalMetrics(startDate, endDate);
            return ApiResponse.success("获取经营指标成功", metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取经营指标失败: " + e.getMessage());
        }
    }
}
