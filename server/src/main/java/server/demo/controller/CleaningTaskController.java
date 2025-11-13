package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.CleaningTaskCreateDTO;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.CleaningTaskUpdateDTO;
import server.demo.service.CleaningTaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 保洁任务Controller
 */
@RestController
@RequestMapping("/api/v1/cleaning-tasks")
public class CleaningTaskController {

    @Autowired
    private CleaningTaskService cleaningTaskService;

    /**
     * 创建保洁任务
     */
    @PostMapping
    public ApiResponse<CleaningTaskDTO> createTask(@Valid @RequestBody CleaningTaskCreateDTO createDTO) {
        try {
            CleaningTaskDTO task = cleaningTaskService.createTask(createDTO);
            return ApiResponse.success("创建任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建任务
     */
    @PostMapping("/batch")
    public ApiResponse<List<CleaningTaskDTO>> batchCreateTasks(
            @Valid @RequestBody List<CleaningTaskCreateDTO> createDTOs) {
        try {
            List<CleaningTaskDTO> tasks = cleaningTaskService.batchCreateTasks(createDTOs);
            return ApiResponse.success("批量创建任务成功", tasks);
        } catch (Exception e) {
            return ApiResponse.error("批量创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public ApiResponse<CleaningTaskDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody CleaningTaskUpdateDTO updateDTO) {
        try {
            CleaningTaskDTO task = cleaningTaskService.updateTask(id, updateDTO);
            return ApiResponse.success("更新任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("更新任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTask(@PathVariable Long id) {
        try {
            cleaningTaskService.deleteTask(id);
            return ApiResponse.success("删除任务成功");
        } catch (Exception e) {
            return ApiResponse.error("删除任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public ApiResponse<CleaningTaskDTO> getTaskById(@PathVariable Long id) {
        try {
            CleaningTaskDTO task = cleaningTaskService.getTaskById(id);
            return ApiResponse.success("获取任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("获取任务失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询任务列表
     */
    @GetMapping
    public ApiResponse<Page<CleaningTaskDTO>> getTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long cleanerId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "taskDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<CleaningTaskDTO> tasks = cleaningTaskService.getTasksWithFilters(
                    userId, startDate, endDate, status, taskType, roomId, cleanerId, roomTypeId, search, pageable
            );
            return ApiResponse.success("获取任务列表成功", tasks);
        } catch (Exception e) {
            return ApiResponse.error("获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取日历视图数据
     */
    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> getCalendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Map<String, Object> data = cleaningTaskService.getCalendarViewData(userId, startDate, endDate, status);
            return ApiResponse.success("获取日历数据成功", data);
        } catch (Exception e) {
            return ApiResponse.error("获取日历数据失败: " + e.getMessage());
        }
    }

    /**
     * 分配任务
     */
    @PostMapping("/{id}/assign")
    public ApiResponse<CleaningTaskDTO> assignTask(
            @PathVariable Long id,
            @RequestParam Long cleanerId) {
        try {
            CleaningTaskDTO task = cleaningTaskService.assignTask(id, cleanerId);
            return ApiResponse.success("分配任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("分配任务失败: " + e.getMessage());
        }
    }

    /**
     * 开始任务
     */
    @PostMapping("/{id}/start")
    public ApiResponse<CleaningTaskDTO> startTask(@PathVariable Long id) {
        try {
            CleaningTaskDTO task = cleaningTaskService.startTask(id);
            return ApiResponse.success("开始任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("开始任务失败: " + e.getMessage());
        }
    }

    /**
     * 完成任务
     */
    @PostMapping("/{id}/complete")
    public ApiResponse<CleaningTaskDTO> completeTask(
            @PathVariable Long id,
            @RequestParam Long approverId) {
        try {
            CleaningTaskDTO task = cleaningTaskService.completeTask(id, approverId);
            return ApiResponse.success("完成任务成功", task);
        } catch (Exception e) {
            return ApiResponse.error("完成任务失败: " + e.getMessage());
        }
    }

    /**
     * 统计任务状态数量
     */
    @GetMapping("/statistics/status")
    public ApiResponse<Map<String, Long>> getStatusCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Map<String, Long> statusCount = cleaningTaskService.getTaskStatusCount(userId, startDate, endDate);
            return ApiResponse.success("获取统计数据成功", statusCount);
        } catch (Exception e) {
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        }
    }
}
