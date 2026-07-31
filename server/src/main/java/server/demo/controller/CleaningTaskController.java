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
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.CleaningTaskCreateDTO;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.CleaningTaskGenerateResult;
import server.demo.dto.CleaningTaskUpdateDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.CleaningTaskAutoService;
import server.demo.service.CleaningTaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import server.demo.i18n.ApiMessages;
/**
 * 保洁任务Controller
 */
@RestController
@RequestMapping("/api/v1/cleaning-tasks")
@StoreScoped
public class CleaningTaskController {

    @Autowired
    private CleaningTaskService cleaningTaskService;

    @Autowired
    private CleaningTaskAutoService cleaningTaskAutoService;

    /**
     * 创建保洁任务
     */
    @PostMapping
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> createTask(@Valid @RequestBody CleaningTaskCreateDTO createDTO) {
        try {
            CleaningTaskDTO task = cleaningTaskService.createTask(createDTO);
            return ApiResponse.success(ApiMessages.get("api.t.9ed9229d5861"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.b41bcb0a7ede") + e.getMessage());
        }
    }

    /**
     * 批量创建任务
     */
    @PostMapping("/batch")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<List<CleaningTaskDTO>> batchCreateTasks(
            @Valid @RequestBody List<CleaningTaskCreateDTO> createDTOs) {
        try {
            List<CleaningTaskDTO> tasks = cleaningTaskService.batchCreateTasks(createDTOs);
            return ApiResponse.success(ApiMessages.get("api.t.7ebfead6b5c7"), tasks);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.0a14fcf9678d") + e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody CleaningTaskUpdateDTO updateDTO) {
        try {
            CleaningTaskDTO task = cleaningTaskService.updateTask(id, updateDTO);
            return ApiResponse.success(ApiMessages.get("api.t.ebd5016a05b8"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.197491425314") + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<String> deleteTask(@PathVariable Long id) {
        try {
            cleaningTaskService.deleteTask(id);
            return ApiResponse.success(ApiMessages.get("api.t.f66c88943a2b"));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.0903888d291a") + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> getTaskById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = resolveCleanerScopedUserId(request);
            CleaningTaskDTO task = cleaningTaskService.getTaskById(userId, id);
            return ApiResponse.success(ApiMessages.get("api.t.856656c369f2"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.821acade7ddf") + e.getMessage());
        }
    }

    /**
     * 分页查询任务列表
     */
    @GetMapping
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
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
            Long userId = resolveCleanerScopedUserId(request);
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<CleaningTaskDTO> tasks = cleaningTaskService.getTasksWithFilters(
                    userId, startDate, endDate, status, taskType, roomId, cleanerId, roomTypeId, search, pageable
            );
            return ApiResponse.success(ApiMessages.get("api.t.3c829403eb9b"), tasks);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.a7e16ca17736") + e.getMessage());
        }
    }

    /**
     * 获取日历视图数据
     */
    @GetMapping("/calendar")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<Map<String, Object>> getCalendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cleanerId,
            HttpServletRequest request) {
        try {
            Long userId = resolveCleanerScopedUserId(request);
            Map<String, Object> data = cleaningTaskService.getCalendarViewData(
                    userId,
                    startDate,
                    endDate,
                    status,
                    cleanerId
            );
            return ApiResponse.success(ApiMessages.get("api.t.bea1c807e285"), data);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.4cf9fb336e8c") + e.getMessage());
        }
    }

    /**
     * 补齐指定日期范围的保洁任务（基于预订离店日期）
     */
    @PostMapping("/generate")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskGenerateResult> generateTasks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            CleaningTaskGenerateResult result = cleaningTaskAutoService.generateTasksForRange(startDate, endDate);
            return ApiResponse.success(ApiMessages.get("api.t.5cda1d095a5d"), result);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.ebe6f1396af8") + e.getMessage());
        }
    }

    /**
     * 分配任务
     */
    @PostMapping("/{id}/assign")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> assignTask(
            @PathVariable Long id,
            @RequestParam Long cleanerId) {
        try {
            CleaningTaskDTO task = cleaningTaskService.assignTask(id, cleanerId);
            return ApiResponse.success(ApiMessages.get("api.t.8882fd1592b4"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.a36ed550d906") + e.getMessage());
        }
    }

    /**
     * 接受任务（保洁员）
     */
    @PostMapping("/{id}/accept")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> acceptTask(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            CleaningTaskDTO task = cleaningTaskService.acceptTask(userId, id);
            return ApiResponse.success(ApiMessages.get("api.t.d860a515d2a2"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.572f97313866") + e.getMessage());
        }
    }

    /**
     * 拒绝任务（保洁员）
     */
    @PostMapping("/{id}/reject")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> rejectTask(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            CleaningTaskDTO task = cleaningTaskService.rejectTask(userId, id);
            return ApiResponse.success(ApiMessages.get("api.t.e8761f0d9c89"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.701be1771640") + e.getMessage());
        }
    }

    /**
     * 开始任务
     */
    @PostMapping("/{id}/start")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> startTask(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            CleaningTaskDTO task = cleaningTaskService.startTask(userId, id);
            return ApiResponse.success(ApiMessages.get("api.t.126a0bca822e"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.0a11e3632e98") + e.getMessage());
        }
    }

    /**
     * 完成任务
     */
    @PostMapping("/{id}/complete")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.TASK_LIST)
    public ApiResponse<CleaningTaskDTO> completeTask(
            @PathVariable Long id,
            @RequestParam(required = false) Long approverId,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            CleaningTaskDTO task = cleaningTaskService.completeTask(userId, id, approverId);
            return ApiResponse.success(ApiMessages.get("api.t.47e6265bdb1a"), task);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.6ce9fabd345c") + e.getMessage());
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
            return ApiResponse.success(ApiMessages.get("api.t.a50e8d0dab1f"), statusCount);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.9bde0b59ef4c") + e.getMessage());
        }
    }

    private Long resolveCleanerScopedUserId(HttpServletRequest request) {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || !isCleanerMemberRole(context.getRole())) {
            return null;
        }

        Object userId = request.getAttribute("userId");
        if (userId instanceof Long value) {
            return value;
        }
        return null;
    }

    private boolean isCleanerMemberRole(String role) {
        return role != null && "member".equalsIgnoreCase(role);
    }
}
