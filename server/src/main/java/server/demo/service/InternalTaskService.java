package server.demo.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.dto.internaltask.*;
import server.demo.entity.*;
import server.demo.enums.InternalTaskStatus;
import server.demo.exception.*;
import server.demo.repository.*;
import server.demo.util.StoreContextUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InternalTaskService {
    private final InternalTaskRepository taskRepository;
    private final StoreUserRepository storeUserRepository;
    private final UserRepository userRepository;
    private final CleanerRepository cleanerRepository;

    public InternalTaskService(InternalTaskRepository taskRepository, StoreUserRepository storeUserRepository,
                               UserRepository userRepository, CleanerRepository cleanerRepository) {
        this.taskRepository = taskRepository; this.storeUserRepository = storeUserRepository;
        this.userRepository = userRepository; this.cleanerRepository = cleanerRepository;
    }

    @Transactional(readOnly = true)
    public InternalTaskPageDTO getMine(InternalTaskStatus status, int page, int size) {
        StoreContext context = requireActiveContext();
        if (status != InternalTaskStatus.ASSIGNED && status != InternalTaskStatus.COMPLETED) {
            throw new IllegalArgumentException("个人任务仅支持 ASSIGNED 或 COMPLETED 状态");
        }
        Sort sort = status == InternalTaskStatus.COMPLETED
                ? Sort.by(Sort.Direction.DESC, "completedAt") : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size), sort);
        Page<InternalTask> result = taskRepository
                .findByStoreIdAndAssigneeUserIdAndStatusAndArchivedAtIsNull(
                        context.getStoreId(), context.getUserId(), status, pageable);
        boolean manager = isManager(context);
        List<InternalTaskDTO> items = result.map(task -> toDto(task, context, manager)).getContent();
        long assigned = taskRepository.countByStoreIdAndAssigneeUserIdAndStatusAndArchivedAtIsNull(
                context.getStoreId(), context.getUserId(), InternalTaskStatus.ASSIGNED);
        long completed = taskRepository.countByStoreIdAndAssigneeUserIdAndStatusAndArchivedAtIsNull(
                context.getStoreId(), context.getUserId(), InternalTaskStatus.COMPLETED);
        return new InternalTaskPageDTO(items, result.getNumber(), result.getSize(), result.getTotalElements(), assigned, completed);
    }

    @Transactional(readOnly = true)
    public InternalTaskPageDTO getManaged(InternalTaskStatus status, int page, int size) {
        StoreContext context = requireManager();
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InternalTask> result = status == null
                ? taskRepository.findByStoreIdAndArchivedAtIsNull(context.getStoreId(), pageable)
                : taskRepository.findByStoreIdAndStatusAndArchivedAtIsNull(context.getStoreId(), status, pageable);
        List<InternalTaskDTO> items = result.map(task -> toDto(task, context, true)).getContent();
        long assigned = taskRepository.countByStoreIdAndStatusAndArchivedAtIsNull(
                context.getStoreId(), InternalTaskStatus.ASSIGNED);
        long completed = taskRepository.countByStoreIdAndStatusAndArchivedAtIsNull(
                context.getStoreId(), InternalTaskStatus.COMPLETED);
        return new InternalTaskPageDTO(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                assigned,
                completed
        );
    }

    @Transactional(readOnly = true)
    public InternalTaskDTO getById(Long id) {
        StoreContext context = requireActiveContext();
        InternalTask task = scopedTask(id, context.getStoreId());
        boolean manager = isManager(context);
        if (!manager && !Objects.equals(task.getAssigneeUserId(), context.getUserId())) {
            throw new InternalTaskNotFoundException("任务不存在");
        }
        return toDto(task, context, manager);
    }

    @Transactional
    public InternalTaskDTO create(InternalTaskCreateRequest request) {
        StoreContext context = requireManager();
        String title = request == null || request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isEmpty() || title.length() > 160) throw new IllegalArgumentException("任务标题不能为空且不能超过160个字符");
        User actor = activeUser(context.getUserId());
        InternalTask task = new InternalTask(); task.setStoreId(context.getStoreId()); task.setTitle(title);
        task.setDescription(normalizeDescription(request.getDescription())); task.setCreatedByUserId(actor.getId());
        task.setCreatedByNameSnapshot(displayName(actor));
        applyAssignee(task, request.getAssigneeUserId(), context.getStoreId());
        return toDto(taskRepository.save(task), context, true);
    }

    @Transactional
    public InternalTaskDTO assign(Long id, InternalTaskAssignRequest request) {
        StoreContext context = requireManager();
        InternalTask task = scopedTask(id, context.getStoreId());
        if (task.getArchivedAt() != null) throw new InternalTaskConflictException("任务已归档");
        if (task.getStatus() == InternalTaskStatus.COMPLETED) throw new InternalTaskConflictException("已完成任务不能改派");
        if (request != null && request.getVersion() != null && !Objects.equals(request.getVersion(), task.getVersion())) {
            throw new InternalTaskConflictException("任务已被其他人更新，请刷新后重试");
        }
        applyAssignee(task, request == null ? null : request.getAssigneeUserId(), context.getStoreId());
        return toDto(taskRepository.save(task), context, true);
    }

    @Transactional
    public InternalTaskDTO complete(Long id) {
        StoreContext context = requireActiveContext(); User actor = activeUser(context.getUserId());
        LocalDateTime now = LocalDateTime.now();
        int changed = taskRepository.completeAssigned(id, context.getStoreId(), context.getUserId(), displayName(actor), now,
                InternalTaskStatus.ASSIGNED, InternalTaskStatus.COMPLETED);
        InternalTask task = scopedTask(id, context.getStoreId());
        if (changed == 0) {
            if (!Objects.equals(task.getAssigneeUserId(), context.getUserId())) throw new InternalTaskNotFoundException("任务不存在");
            if (task.getArchivedAt() != null) throw new InternalTaskConflictException("任务已归档");
            if (task.getStatus() != InternalTaskStatus.COMPLETED) throw new InternalTaskConflictException("任务当前不可完成");
        }
        return toDto(task, context, isManager(context));
    }

    @Transactional
    public InternalTaskDTO archive(Long id) {
        StoreContext context = requireManager(); InternalTask task = scopedTask(id, context.getStoreId());
        if (task.getArchivedAt() == null) { task.setArchivedAt(LocalDateTime.now()); task.setArchivedByUserId(context.getUserId()); taskRepository.save(task); }
        return toDto(task, context, true);
    }

    @Transactional(readOnly = true)
    public List<InternalTaskAssigneeDTO> getAssignees() {
        StoreContext context = requireManager(); Long storeId = context.getStoreId();
        Map<Long, Cleaner> activeCleaners = validateCleanerIdentities(storeId);
        List<InternalTaskAssigneeDTO> result = new ArrayList<>();
        for (StoreUser membership : storeUserRepository.findActiveUsersByStoreId(storeId)) {
            User user = membership.getUser();
            if (user == null || user.getId() == null || !Boolean.TRUE.equals(user.getIsActive())) continue;
            Cleaner cleaner = activeCleaners.get(user.getId());
            result.add(new InternalTaskAssigneeDTO(user.getId(), displayName(user), user.getAvatar(), membership.getRole(),
                    cleaner == null ? "STAFF" : "CLEANER", cleaner == null ? null : cleaner.getId()));
        }
        result.sort(Comparator.comparing(InternalTaskAssigneeDTO::getDisplayName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    private Map<Long, Cleaner> validateCleanerIdentities(Long storeId) {
        Map<Long, Cleaner> result = new HashMap<>(); List<String> errors = new ArrayList<>();
        for (Cleaner cleaner : cleanerRepository.findByStoreIdAndIsActiveTrue(storeId)) {
            if (cleaner.getUserId() == null) { errors.add("cleanerId=" + cleaner.getId() + " 未绑定用户"); continue; }
            User user = userRepository.findById(cleaner.getUserId()).orElse(null);
            StoreUser membership = storeUserRepository.findByStoreIdAndUserId(storeId, cleaner.getUserId()).orElse(null);
            if (user == null) errors.add("cleanerId=" + cleaner.getId() + " 绑定用户不存在");
            else if (!emailEquals(user.getEmail(), cleaner.getEmail())) errors.add("cleanerId=" + cleaner.getId() + " 与用户邮箱不一致");
            else if (!Boolean.TRUE.equals(user.getIsActive())) errors.add("cleanerId=" + cleaner.getId() + " 绑定用户已停用");
            else if (membership == null || !Boolean.TRUE.equals(membership.getIsActive())) errors.add("cleanerId=" + cleaner.getId() + " 缺少有效门店成员关系");
            if (result.putIfAbsent(cleaner.getUserId(), cleaner) != null) errors.add("userId=" + cleaner.getUserId() + " 存在重复保洁身份");
        }
        if (!errors.isEmpty()) throw new InternalTaskConflictException("保洁员身份数据不完整，暂不能分配任务：" + String.join("；", errors));
        return result;
    }

    private void applyAssignee(InternalTask task, Long assigneeUserId, Long storeId) {
        if (assigneeUserId == null) { task.setAssigneeUserId(null); task.setAssigneeNameSnapshot(null); task.setStatus(InternalTaskStatus.UNASSIGNED); return; }
        User user = activeUser(assigneeUserId);
        StoreUser membership = storeUserRepository.findByStoreIdAndUserId(storeId, assigneeUserId)
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).orElseThrow(() -> new InternalTaskConflictException("执行人不是当前门店有效员工"));
        List<Cleaner> cleaners = cleanerRepository.findByUserIdAndStoreId(assigneeUserId, storeId);
        if (cleaners.size() > 1) throw new InternalTaskConflictException("执行人在当前门店存在重复保洁身份");
        if (!cleaners.isEmpty()) {
            Cleaner cleaner = cleaners.get(0);
            if (!Boolean.TRUE.equals(cleaner.getIsActive()) || !emailEquals(cleaner.getEmail(), user.getEmail())) {
                throw new InternalTaskConflictException("执行人的保洁身份无效");
            }
        }
        task.setAssigneeUserId(membership.getUser().getId()); task.setAssigneeNameSnapshot(displayName(user));
        task.setStatus(InternalTaskStatus.ASSIGNED);
    }

    private StoreContext requireActiveContext() {
        StoreContext context = StoreContextUtils.requireContext(); activeUser(context.getUserId());
        storeUserRepository.findByStoreIdAndUserId(context.getStoreId(), context.getUserId())
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .orElseThrow(() -> new PermissionDeniedException("当前门店成员关系已停用"));
        return context;
    }
    private StoreContext requireManager() { StoreContext context = requireActiveContext(); if (!isManager(context)) throw new PermissionDeniedException("仅门店所有者或管理员可管理内部任务"); return context; }
    private boolean isManager(StoreContext context) { return "owner".equalsIgnoreCase(context.getRole()) || "admin".equalsIgnoreCase(context.getRole()); }
    private User activeUser(Long id) { return userRepository.findById(id).filter(user -> Boolean.TRUE.equals(user.getIsActive())).orElseThrow(() -> new PermissionDeniedException("用户账号已停用或不存在")); }
    private InternalTask scopedTask(Long id, Long storeId) { return taskRepository.findByIdAndStoreId(id, storeId).orElseThrow(() -> new InternalTaskNotFoundException("任务不存在")); }
    private InternalTaskDTO toDto(InternalTask task, StoreContext context, boolean manager) { boolean canComplete = task.getArchivedAt() == null && task.getStatus() == InternalTaskStatus.ASSIGNED && Objects.equals(task.getAssigneeUserId(), context.getUserId()); return InternalTaskDTO.from(task, canComplete, manager); }
    private int safePage(int page) { return Math.max(0, page); } private int safeSize(int size) { return Math.max(1, Math.min(size, 100)); }
    private String normalizeDescription(String value) { if (value == null || value.trim().isEmpty()) return null; String result = value.trim(); if (result.length() > 4000) throw new IllegalArgumentException("任务说明不能超过4000个字符"); return result; }
    private String displayName(User user) { if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname(); if (user.getName() != null && !user.getName().isBlank()) return user.getName(); return user.getEmail(); }
    private boolean emailEquals(String left, String right) { return left != null && right != null && left.trim().equalsIgnoreCase(right.trim()); }
}
