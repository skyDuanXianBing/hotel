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
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.service.CleaningTaskAutoService;
import server.demo.service.CleanerIdentityService;
import server.demo.service.CleaningTaskService;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private CleaningTaskAutoService cleaningTaskAutoService;

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    @Override
    @Transactional
    public CleaningTaskDTO createTask(CleaningTaskCreateDTO createDTO) {
        Room room = roomRepository.findById(createDTO.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        CleaningTask task = new CleaningTask();

        // 将字符串日期转换为LocalDate
        task.setTaskDate(LocalDate.parse(createDTO.getTaskDate()));
        task.setRoom(room);
        task.setTaskType(createDTO.getTaskType());
        task.setStatus("pending");
        task.setSource("manual");

        // estimatedTime暂时不设置,因为前端发送的是 "HH:MM-HH:MM" 格式,不是完整的日期时间
        // TODO: 如果需要,可以在前端改为发送完整的日期时间,或者在这里构造
        // task.setEstimatedTime(createDTO.getEstimatedTime());

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
    public CleaningTaskDTO getTaskById(Long userId, Long id) {
        CleaningTask task = cleaningTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        if (currentCleaner.isPresent()) {
            ensureCleanerOwnsTask(task, currentCleaner.get());
        }
        return convertToDTO(task);
    }

    @Override
    @Transactional
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
        // 获取当前门店ID
        Long storeId = getCurrentStoreId();
        cleaningTaskAutoService.markExpiredTasks(storeId, storeToday(storeId));

        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        Long effectiveCleanerId = cleanerId;
        if (currentCleaner.isPresent()) {
            effectiveCleanerId = currentCleaner.get().getId();
        }

        Page<CleaningTask> tasks = cleaningTaskRepository.findWithFiltersByStore(
                storeId, startDate, endDate, status, taskType, roomId, effectiveCleanerId, roomTypeId, pageable
        );

        return tasks.map(this::convertToDTO);
    }

    /**
     * 获取当前门店ID
     */
    private Long getCurrentStoreId() {
        server.demo.context.StoreContext storeContext = server.demo.context.StoreContextHolder.getContext();
        if (storeContext == null || storeContext.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        return storeContext.getStoreId();
    }

    @Override
    @Transactional
    public Map<String, Object> getCalendarViewData(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Long cleanerId
    ) {
        // 获取当前门店ID
        Long storeId = getCurrentStoreId();
        cleaningTaskAutoService.markExpiredTasks(storeId, storeToday(storeId));
        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        Long effectiveCleanerId = cleanerId;
        if (currentCleaner.isPresent()) {
            effectiveCleanerId = currentCleaner.get().getId();
        }

        List<CleaningTask> tasks;

        if ((status != null && !status.isEmpty()) || effectiveCleanerId != null) {
            // 如果指定了状态,需要筛选
            tasks = cleaningTaskRepository.findWithFiltersByStore(
                    storeId, startDate, endDate, status, null, null, effectiveCleanerId, null, Pageable.unpaged()
            ).getContent();
        } else {
            // 否则查询所有
            tasks = cleaningTaskRepository.findByTaskDateBetweenAndStoreId(storeId, startDate, endDate);
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
    public CleaningTaskDTO acceptTask(Long userId, Long taskId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        if (currentCleaner.isPresent()) {
            ensureCleanerOwnsTask(task, currentCleaner.get());
        }

        if (!"assigned".equals(task.getStatus())) {
            throw new RuntimeException("只有已分配的任务才能接受");
        }

        task.setStatus("in_progress");

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO rejectTask(Long userId, Long taskId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        if (currentCleaner.isPresent()) {
            ensureCleanerOwnsTask(task, currentCleaner.get());
        }

        if (!"assigned".equals(task.getStatus())) {
            throw new RuntimeException("只有已分配的任务才能拒绝");
        }

        // 拒绝后将任务状态改回待分配，并清除保洁员
        task.setStatus("pending");
        task.setCleaner(null);

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO startTask(Long userId, Long taskId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        if (currentCleaner.isPresent()) {
            ensureCleanerOwnsTask(task, currentCleaner.get());
        }

        task.setStatus("in_progress");
        task.setStartTime(LocalDateTime.now());

        CleaningTask updatedTask = cleaningTaskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public CleaningTaskDTO completeTask(Long userId, Long taskId, Long approverId) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        Optional<Cleaner> currentCleaner = findCurrentCleaner(userId);
        if (currentCleaner.isPresent()) {
            ensureCleanerOwnsTask(task, currentCleaner.get());
            approverId = userId;
        }
        if (approverId == null) {
            throw new RuntimeException("审批人不存在");
        }

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
            // 将字符串日期转换为LocalDate
            task.setTaskDate(LocalDate.parse(createDTO.getTaskDate()));
            task.setRoom(room);
            task.setTaskType(createDTO.getTaskType());
            task.setStatus("pending");
            task.setSource("manual");
            // estimatedTime暂时不设置
            // task.setEstimatedTime(createDTO.getEstimatedTime());
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
        // 获取当前门店ID
        Long storeId = getCurrentStoreId();

        List<Object[]> results = cleaningTaskRepository.countByStatusInDateRangeByStore(storeId, startDate, endDate);

        Map<String, Long> statusCount = new HashMap<>();
        for (Object[] result : results) {
            String status = (String) result[0];
            Long count = (Long) result[1];
            statusCount.put(status, count);
        }

        return statusCount;
    }

    private Optional<Cleaner> findCurrentCleaner(Long userId) {
        Long storeId = getCurrentStoreId();
        return cleanerIdentityService.findCleanerByUserIdAndStoreId(userId, storeId);
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

    private void ensureCleanerOwnsTask(CleaningTask task, Cleaner cleaner) {
        if (task.getCleaner() == null || !Objects.equals(task.getCleaner().getId(), cleaner.getId())) {
            throw new RuntimeException("只能查看或操作分配给自己的任务");
        }
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
        dto.setReservationId(task.getReservationId());
        dto.setSource(task.getSource());
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
