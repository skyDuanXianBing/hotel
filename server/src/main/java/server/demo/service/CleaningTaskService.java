package server.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.demo.dto.CleaningTaskCreateDTO;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.CleaningTaskUpdateDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 保洁任务Service接口
 */
public interface CleaningTaskService {

    /**
     * 创建保洁任务
     */
    CleaningTaskDTO createTask(CleaningTaskCreateDTO createDTO);

    /**
     * 更新保洁任务
     */
    CleaningTaskDTO updateTask(Long id, CleaningTaskUpdateDTO updateDTO);

    /**
     * 删除保洁任务
     */
    void deleteTask(Long id);

    /**
     * 根据ID获取任务详情
     */
    CleaningTaskDTO getTaskById(Long id);

    /**
     * 分页查询任务列表(支持多条件筛选)
     */
    Page<CleaningTaskDTO> getTasksWithFilters(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String taskType,
            Long roomId,
            Long cleanerId,
            Long roomTypeId,
            String search,
            Pageable pageable
    );

    /**
     * 获取日历视图数据(按日期和房间分组)
     */
    Map<String, Object> getCalendarViewData(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Long cleanerId
    );

    /**
     * 分配任务给保洁员
     */
    CleaningTaskDTO assignTask(Long taskId, Long cleanerId);

    /**
     * 保洁员接受任务
     */
    CleaningTaskDTO acceptTask(Long taskId);

    /**
     * 保洁员拒绝任务
     */
    CleaningTaskDTO rejectTask(Long taskId);

    /**
     * 开始任务
     */
    CleaningTaskDTO startTask(Long taskId);

    /**
     * 完成任务
     */
    CleaningTaskDTO completeTask(Long taskId, Long approverId);

    /**
     * 批量创建任务
     */
    List<CleaningTaskDTO> batchCreateTasks(List<CleaningTaskCreateDTO> createDTOs);

    /**
     * 统计任务状态数量
     */
    Map<String, Long> getTaskStatusCount(Long userId, LocalDate startDate, LocalDate endDate);
}
