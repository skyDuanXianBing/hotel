package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.BusinessSummaryDTO;
import server.demo.dto.DailyOccupancyDTO;
import server.demo.service.BusinessStatisticsService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics/business")
@CrossOrigin
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
}