package server.demo.controller;

import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.internaltask.*;
import server.demo.enums.InternalTaskStatus;
import server.demo.service.InternalTaskService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal-tasks")
@StoreScoped
public class InternalTaskController {
    private final InternalTaskService service;
    public InternalTaskController(InternalTaskService service) { this.service = service; }

    @GetMapping public ApiResponse<InternalTaskPageDTO> list(@RequestParam(required = false) InternalTaskStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.getManaged(status, page, size));
    }
    @GetMapping("/mine") public ApiResponse<InternalTaskPageDTO> mine(
            @RequestParam(defaultValue = "ASSIGNED") InternalTaskStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.getMine(status, page, size));
    }
    @GetMapping("/assignees") public ApiResponse<List<InternalTaskAssigneeDTO>> assignees() { return ApiResponse.success(service.getAssignees()); }
    @GetMapping("/{id}") public ApiResponse<InternalTaskDTO> detail(@PathVariable Long id) { return ApiResponse.success(service.getById(id)); }
    @PostMapping public ApiResponse<InternalTaskDTO> create(@RequestBody InternalTaskCreateRequest request) { return ApiResponse.success("创建任务成功", service.create(request)); }
    @PutMapping("/{id}/assignee") public ApiResponse<InternalTaskDTO> assign(@PathVariable Long id, @RequestBody InternalTaskAssignRequest request) { return ApiResponse.success("分配任务成功", service.assign(id, request)); }
    @PostMapping("/{id}/complete") public ApiResponse<InternalTaskDTO> complete(@PathVariable Long id) { return ApiResponse.success("任务已完成", service.complete(id)); }
    @PostMapping("/{id}/archive") public ApiResponse<InternalTaskDTO> archive(@PathVariable Long id) { return ApiResponse.success("任务已归档", service.archive(id)); }
}
