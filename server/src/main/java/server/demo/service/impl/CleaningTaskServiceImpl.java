package server.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CleaningTaskCreateDTO;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.CleaningTaskUpdateDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleaningTask;
import server.demo.entity.Room;
import server.demo.entity.User;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.UserRepository;
import server.demo.service.CleaningTaskService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 保洁任务Service实现类
 */
@Service
public class CleaningTaskServiceImpl implements CleaningTaskService {

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public CleaningTaskDTO createTask(CleaningTaskCreateDTO createDTO) {
        Room room = roomRepository.findById(createDTO.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        CleaningTask task = new CleaningTask();
        task.setTaskDate(createDTO.getTaskDate());
        task.setRoom(room);
        task.setTaskType(createDTO.getTaskType());
        task.setStatus("pending");
        task.setEstimatedTime(createDTO.getEstimatedTime());
        task.setNotes(createDTO.getNotes());

        if (createDTO.getCleanerId() != null) {
            Cleaner cleaner = cleanerRepository.findById(createDTO.getCleanerId())
                    .orElseThrow(() -> new RuntimeException("保洁员不存在"));
            task.setCleaner(cleaner);
            task.setStatus("assigned");
        }

        CleaningTask savedTask = cleaningTaskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO updateTask(Long id, CleaningTaskUpdateDTO updateDTO) {
        CleaningTask task = cleaningTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (updateDTO.getStatus() != null) {
            task.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getCleanerId() != null) {
            Cleaner cleaner = cleanerRepository.findById(updateDTO.getCleanerId())
                    .orElseThrow(() -> new RuntimeException("保洁员不存在"));
            task.setCleaner(cleaner);
        }
        if (updateDTO.getEstimatedTime() != null) {
            task.setEstimatedTime(updateDTO.getEstimatedTime());
        }
        if (updateDTO.getStartTime() != null) {
            task.setStartTime(updateDTO.getStartTime());
        }
        if (updateDTO.getCompleteTime() != null) {
            task.setCompleteTime(updateDTO.getCompleteTime());
        }
        if (updateDTO.getApproverId() != null) {
            User approver = userRepository.findById(updateDTO.getApproverId())
                    .orElseThrow(() -> new RuntimeException("审批人不存在"));
            task.setApprover(approver);
        }
        if (updateDTO.getNotes() != null) {
            task.setNotes(updateDTO.getNotes());
        }

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!cleaningTaskRepository.existsById(id)) {
            throw new RuntimeException("任务不存在");
        }
        cleaningTaskRepository.deleteById(id);
    }

    @Override
    public CleaningTaskDTO getTaskById(Long id) {
        CleaningTask task = cleaningTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        return convertToDTO(task);
    }

    @Override
    public Page<CleaningTaskDTO> getTasksWithFilters(
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
    ) {
        Page<CleaningTask> tasks = cleaningTaskRepository.findWithFilters(
                userId, startDate, endDate, status, taskType, roomId, cleanerId, roomTypeId, pageable
        );

        return tasks.map(this::convertToDTO);
    }

    @Override
    public Map<String, Object> getCalendarViewData(Long userId, LocalDate startDate, LocalDate endDate, String status) {
        List<CleaningTask> tasks;

        if (status != null && !status.isEmpty()) {
            // 如果指定了状态,需要筛选
            tasks = cleaningTaskRepository.findWithFilters(
                    userId, startDate, endDate, status, null, null, null, null, Pageable.unpaged()
            ).getContent();
        } else {
            // 否则查询所有
            tasks = cleaningTaskRepository.findByTaskDateBetweenAndUserId(userId, startDate, endDate);
        }

        // 按房间和日期分组
        Map<String, List<CleaningTaskDTO>> groupedTasks = tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.groupingBy(task ->
                    task.getRoomId() + "_" + task.getTaskDate()
                ));

        // 构建日历数据
        Map<String, Object> result = new HashMap<>();
        result.put("tasks", groupedTasks);
        result.put("totalCount", tasks.size());

        // 统计各状态数量
        Map<String, Long> statusCount = tasks.stream()
                .collect(Collectors.groupingBy(CleaningTask::getStatus, Collectors.counting()));
        result.put("statusCount", statusCount);

        return result;
    }

    @Override
    @Transactional
    public CleaningTaskDTO assignTask(Long taskId, Long cleanerId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        Cleaner cleaner = cleanerRepository.findById(cleanerId)
                .orElseThrow(() -> new RuntimeException("保洁员不存在"));

        task.setCleaner(cleaner);
        task.setStatus("assigned");

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO startTask(Long taskId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        task.setStatus("in_progress");
        task.setStartTime(LocalDateTime.now());

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO completeTask(Long taskId, Long approverId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在"));

        task.setStatus("completed");
        task.setCompleteTime(LocalDateTime.now());
        task.setApprover(approver);

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public List<CleaningTaskDTO> batchCreateTasks(List<CleaningTaskCreateDTO> createDTOs) {
        List<CleaningTask> tasks = new ArrayList<>();

        for (CleaningTaskCreateDTO createDTO : createDTOs) {
            Room room = roomRepository.findById(createDTO.getRoomId())
                    .orElseThrow(() -> new RuntimeException("房间不存在"));

            CleaningTask task = new CleaningTask();
            task.setTaskDate(createDTO.getTaskDate());
            task.setRoom(room);
            task.setTaskType(createDTO.getTaskType());
            task.setStatus("pending");
            task.setEstimatedTime(createDTO.getEstimatedTime());
            task.setNotes(createDTO.getNotes());

            if (createDTO.getCleanerId() != null) {
                Cleaner cleaner = cleanerRepository.findById(createDTO.getCleanerId())
                        .orElseThrow(() -> new RuntimeException("保洁员不存在"));
                task.setCleaner(cleaner);
                task.setStatus("assigned");
            }

            tasks.add(task);
        }

        List<CleaningTask> savedTasks = cleaningTaskRepository.saveAll(tasks);
        return savedTasks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getTaskStatusCount(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = cleaningTaskRepository.countByStatusInDateRange(userId, startDate, endDate);

        Map<String, Long> statusCount = new HashMap<>();
        for (Object[] result : results) {
            String status = (String) result[0];
            Long count = (Long) result[1];
            statusCount.put(status, count);
        }

        return statusCount;
    }

    /**
     * 转换实体为DTO
     */
    private CleaningTaskDTO convertToDTO(CleaningTask task) {
        CleaningTaskDTO dto = new CleaningTaskDTO();
        dto.setId(task.getId());
        dto.setTaskDate(task.getTaskDate());
        dto.setTaskType(task.getTaskType());
        dto.setStatus(task.getStatus());
        dto.setEstimatedTime(task.getEstimatedTime());
        dto.setStartTime(task.getStartTime());
        dto.setCompleteTime(task.getCompleteTime());
        dto.setNotes(task.getNotes());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        if (task.getRoom() != null) {
            dto.setRoomId(task.getRoom().getId());
            dto.setRoomNumber(task.getRoom().getRoomNumber());
            if (task.getRoom().getRoomType() != null) {
                dto.setRoomType(task.getRoom().getRoomType().getName());
                dto.setRoomTypeId(task.getRoom().getRoomType().getId());
            }
        }

        if (task.getCleaner() != null) {
            dto.setCleanerId(task.getCleaner().getId());
            dto.setCleanerName(task.getCleaner().getName());
        }

        if (task.getApprover() != null) {
            dto.setApproverId(task.getApprover().getId());
            dto.setApproverName(task.getApprover().getName());
        }

        return dto;
    }
}
