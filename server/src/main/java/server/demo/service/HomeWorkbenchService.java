package server.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.home.HomeWorkbenchActionDTO;
import server.demo.dto.home.HomeWorkbenchItemDTO;
import server.demo.dto.home.HomeWorkbenchMetaItemDTO;
import server.demo.dto.home.HomeWorkbenchResponse;
import server.demo.dto.home.HomeWorkbenchStatusSummaryDTO;
import server.demo.dto.home.HomeWorkbenchTargetDTO;
import server.demo.dto.home.HomeWorkbenchTypeSummaryDTO;
import server.demo.dto.internaltask.InternalTaskDTO;
import server.demo.dto.internaltask.InternalTaskPageDTO;
import server.demo.dto.registration.AdminRegistrationListItemDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.Store;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.InternalTaskStatus;
import server.demo.enums.RegistrationFormStatus;
import server.demo.exception.StoreAccessDeniedException;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class HomeWorkbenchService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private static final String TYPE_ALL = "all";
    private static final String TYPE_CLEANING = "cleaning";
    private static final String TYPE_REVIEW = "review";
    private static final String TYPE_ORDER = "order";
    private static final String TYPE_MESSAGE = "message";
    private static final String TYPE_OTHER = "other";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_AWAITING_REVIEW = "awaiting_review";
    private static final String STATUS_AWAITING_REPLY = "awaiting_reply";
    private static final String STATUS_UNASSIGNED = "unassigned";
    private static final String STATUS_ASSIGNED = "assigned";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_OVERDUE = "overdue";
    private static final String STATUS_COMPLETED = "completed";
    private static final int COMPLETED_REVIEW_RETENTION_DAYS = 7;

    private static final String PRIORITY_HIGH = "high";
    private static final String PRIORITY_MEDIUM = "medium";
    private static final String PRIORITY_LOW = "low";

    private final CleaningTaskService cleaningTaskService;
    private final RegistrationAdminService registrationAdminService;
    private final ReservationRepository reservationRepository;
    private final SuMessagingService suMessagingService;
    private final StoreRepository storeRepository;
    private final StoreUserRepository storeUserRepository;
    private final PermissionService permissionService;
    private final InternalTaskService internalTaskService;
    private final Clock clock;

    public HomeWorkbenchService(
            CleaningTaskService cleaningTaskService,
            RegistrationAdminService registrationAdminService,
            ReservationRepository reservationRepository,
            SuMessagingService suMessagingService,
            StoreRepository storeRepository,
            StoreUserRepository storeUserRepository,
            PermissionService permissionService,
            InternalTaskService internalTaskService,
            Clock clock
    ) {
        this.cleaningTaskService = cleaningTaskService;
        this.registrationAdminService = registrationAdminService;
        this.reservationRepository = reservationRepository;
        this.suMessagingService = suMessagingService;
        this.storeRepository = storeRepository;
        this.storeUserRepository = storeUserRepository;
        this.permissionService = permissionService;
        this.internalTaskService = internalTaskService;
        this.clock = clock;
    }

    public HomeWorkbenchResponse getWorkbench(LocalDate requestedDate, Integer requestedLimit) {
        return getWorkbench(requestedDate, requestedLimit, TYPE_ALL);
    }

    public HomeWorkbenchResponse getWorkbench(
            LocalDate requestedDate,
            Integer requestedLimit,
            String requestedType
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        if (!storeUserRepository.existsByStoreIdAndUserIdAndIsActiveTrue(storeId, userId)) {
            throw new StoreAccessDeniedException("当前账号未激活或无权访问该门店");
        }
        LocalDate businessDate = resolveBusinessDate(storeId, requestedDate);
        int limit = normalizeLimit(requestedLimit);
        String selectedType = normalizeTypeFilter(requestedType);

        List<HomeWorkbenchItemDTO> allItems = new ArrayList<>();
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        Map<String, Boolean> typeAvailability = new LinkedHashMap<>();

        boolean canViewCleaning = hasPermission(
                storeId, userId, PermissionModule.ACCOMMODATION, PermissionAction.TASK_LIST);
        typeAvailability.put(TYPE_CLEANING, canViewCleaning);
        List<HomeWorkbenchItemDTO> cleaningItems = canViewCleaning
                ? buildCleaningItems(userId, businessDate) : List.of();
        appendItems(TYPE_CLEANING, cleaningItems, typeCounts, allItems);

        boolean canViewReview = hasPermission(
                storeId, userId, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS);
        typeAvailability.put(TYPE_REVIEW, canViewReview);
        ReviewItems reviewResult = canViewReview
                ? buildReviewItems() : new ReviewItems(List.of(), 0L);
        List<HomeWorkbenchItemDTO> reviewItems = reviewResult.items();
        typeCounts.put(TYPE_REVIEW, reviewResult.awaitingCount());
        allItems.addAll(reviewItems);

        boolean canViewOrder = hasPermission(
                storeId, userId, PermissionModule.ORDER, PermissionAction.VIEW_ORDERS);
        typeAvailability.put(TYPE_ORDER, canViewOrder);
        List<HomeWorkbenchItemDTO> orderItems = canViewOrder
                ? buildOrderItems(storeId, businessDate) : List.of();
        appendItems(TYPE_ORDER, orderItems, typeCounts, allItems);

        SuMessagingThreadPageResponse messagePage = suMessagingService.listAwaitingReplyThreadPage(storeId, 0, limit);
        List<HomeWorkbenchItemDTO> messageItems = buildMessageItems(messagePage);
        typeCounts.put(TYPE_MESSAGE, messagePage.getTotalElements());
        typeAvailability.put(TYPE_MESSAGE, true);
        allItems.addAll(messageItems);

        OtherItems otherResult = buildOtherItems(isManagerContext(), limit);
        List<HomeWorkbenchItemDTO> otherItems = otherResult.items();
        typeCounts.put(TYPE_OTHER, otherResult.pendingCount());
        typeAvailability.put(TYPE_OTHER, true);
        allItems.addAll(otherItems);

        List<HomeWorkbenchItemDTO> selectedItems = selectItems(
                selectedType,
                limit,
                allItems,
                cleaningItems,
                reviewItems,
                orderItems,
                messageItems,
                otherItems
        );

        HomeWorkbenchResponse response = new HomeWorkbenchResponse();
        response.setBusinessDate(businessDate);
        response.setGeneratedAt(LocalDateTime.now(effectiveClock()));
        response.setTypeSummaries(buildTypeSummaries(typeCounts, typeAvailability));
        response.setStatusSummaries(buildStatusSummaries(countStatuses(selectedItems)));
        response.setItems(selectedItems);
        return response;
    }

    private List<HomeWorkbenchItemDTO> buildCleaningItems(Long userId, LocalDate businessDate) {
        Page<CleaningTaskDTO> page = cleaningTaskService.getTasksWithFilters(
                userId,
                businessDate,
                businessDate,
                null,
                null,
                null,
                null,
                null,
                null,
                Pageable.unpaged()
        );

        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (CleaningTaskDTO task : page.getContent()) {
            if (!shouldIncludeCleaningTask(task)) {
                continue;
            }
            HomeWorkbenchItemDTO item = new HomeWorkbenchItemDTO();
            item.setId(TYPE_CLEANING + "-" + task.getId());
            item.setType(TYPE_CLEANING);
            item.setSourceType("cleaning_task");
            item.setSourceId(asString(task.getId()));
            item.setSourceStatus(task.getStatus());
            item.setStatusGroup(resolveCleaningStatusGroup(task.getStatus()));
            item.setPriority(resolveCleaningPriority(task.getStatus()));
            item.setDueAt(resolveCleaningDueAt(task));
            item.setTitle("房间 " + fallback(task.getRoomNumber(), "未命名") + " 保洁");
            item.setSubtitle(joinNonBlank(resolveCleaningTaskTypeLabel(task.getTaskType()), task.getRoomType()));
            item.setAssigneeId(task.getCleanerId());
            item.setAssigneeName(task.getCleanerName());
            addMeta(item, "房间", task.getRoomNumber());
            addMeta(item, "房型", task.getRoomType());
            addMeta(item, "类型", resolveCleaningTaskTypeLabel(task.getTaskType()));
            addMeta(item, "状态", resolveCleaningStatusLabel(task.getStatus()));
            item.setTarget(target(
                    "cleaning_task",
                    "/cleaning-tasks",
                    Map.of(
                            "taskId", asString(task.getId()),
                            "date", businessDate.toString()
                    )
            ));
            item.setActions(buildCleaningActions(task.getStatus()));
            items.add(item);
        }
        return items;
    }

    private ReviewItems buildReviewItems() {
        List<AdminRegistrationListItemDTO> submittedForms = registrationAdminService.list(
                RegistrationFormStatus.SUBMITTED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        LocalDateTime completedSince = LocalDateTime.now(effectiveClock()).minusDays(COMPLETED_REVIEW_RETENTION_DAYS);
        List<AdminRegistrationListItemDTO> completedForms =
                registrationAdminService.listRecentApprovedForHome(completedSince);

        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (AdminRegistrationListItemDTO form : submittedForms) {
            items.add(buildReviewItem(form, false));
        }
        for (AdminRegistrationListItemDTO form : completedForms) {
            items.add(buildReviewItem(form, true));
        }
        return new ReviewItems(items, submittedForms.size());
    }

    private HomeWorkbenchItemDTO buildReviewItem(AdminRegistrationListItemDTO form, boolean completed) {
        HomeWorkbenchItemDTO item = new HomeWorkbenchItemDTO();
        item.setId(TYPE_REVIEW + "-" + form.getFormId());
        item.setType(TYPE_REVIEW);
        item.setSourceType("registration_form");
        item.setSourceId(asString(form.getFormId()));
        item.setSourceStatus(form.getStatus() == null ? null : form.getStatus().name());
        item.setStatusGroup(completed ? STATUS_COMPLETED : STATUS_AWAITING_REVIEW);
        item.setPriority(completed ? PRIORITY_LOW : PRIORITY_HIGH);
        item.setDueAt(completed
                ? firstNonNull(form.getApprovedAt(), form.getUpdatedAt())
                : firstNonNull(form.getSubmittedAt(), form.getUpdatedAt()));
        item.setTitle(fallback(form.getGuestName(), "住客") + " 的登记审核");
        item.setSubtitle(joinNonBlank(form.getOrderNumber(), form.getChannelName()));
        addMeta(item, "订单", form.getOrderNumber());
        addMeta(item, "渠道", form.getChannelName());
        addMeta(item, "入住", formatDateRange(form.getCheckInDate(), form.getCheckOutDate()));
        addMeta(item, "状态", completed ? "已完成" : "待审核");
        if (completed && form.getApprovedAt() != null) {
            addMeta(item, "完成时间", form.getApprovedAt().toString());
        }
        item.setTarget(target(
                "registration_form",
                "/registrations",
                Map.of("formId", asString(form.getFormId()))
        ));
        item.setActions(List.of(new HomeWorkbenchActionDTO("view", "查看登记", "default")));
        return item;
    }

    private List<HomeWorkbenchItemDTO> buildOrderItems(Long storeId, LocalDate businessDate) {
        Map<Long, OrderCandidate> candidates = new LinkedHashMap<>();
        List<Reservation> unassignedReservations =
                reservationRepository.findUnassignedOrUnmappedWithDetailsByStoreId(storeId, businessDate);
        for (Reservation reservation : unassignedReservations) {
            if (reservation.getId() != null) {
                candidates.put(reservation.getId(), new OrderCandidate(reservation, "UNASSIGNED"));
            }
        }

        List<Reservation> pendingReservations =
                reservationRepository.findPendingOrdersWithDetailsByStoreId(storeId, businessDate);
        for (Reservation reservation : pendingReservations) {
            if (reservation.getId() != null && !candidates.containsKey(reservation.getId())) {
                candidates.put(reservation.getId(), new OrderCandidate(reservation, "PENDING"));
            }
        }

        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (OrderCandidate candidate : candidates.values()) {
            Reservation reservation = candidate.reservation();
            String sourceStatus = candidate.sourceStatus();
            HomeWorkbenchItemDTO item = new HomeWorkbenchItemDTO();
            item.setId(TYPE_ORDER + "-" + reservation.getId());
            item.setType(TYPE_ORDER);
            item.setSourceType("reservation");
            item.setSourceId(asString(reservation.getId()));
            item.setSourceStatus(sourceStatus);
            item.setStatusGroup(STATUS_PENDING);
            item.setPriority(resolveOrderPriority(reservation, sourceStatus, businessDate));
            item.setDueAt(reservation.getCheckInDate() == null ? null : reservation.getCheckInDate().atStartOfDay());
            item.setTitle(resolveOrderTitle(reservation, sourceStatus));
            item.setSubtitle(joinNonBlank(reservation.getOrderNumber(), resolveReservationChannelName(reservation)));
            addMeta(item, "客人", reservation.getGuestName());
            addMeta(item, "订单", reservation.getOrderNumber());
            addMeta(item, "渠道", resolveReservationChannelName(reservation));
            addMeta(item, "入住", formatDateRange(reservation.getCheckInDate(), reservation.getCheckOutDate()));
            addMeta(item, "房间", resolveReservationRoomNumber(reservation));
            item.setTarget(target(
                    "reservation",
                    "/order",
                    Map.of(
                            "reservationId", asString(reservation.getId()),
                            "type", sourceStatus.toLowerCase()
                    )
            ));
            item.setActions(buildOrderActions(sourceStatus));
            items.add(item);
        }
        return items;
    }

    private List<HomeWorkbenchItemDTO> buildMessageItems(SuMessagingThreadPageResponse messagePage) {
        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        if (messagePage.getItems() == null) {
            return items;
        }
        for (SuMessagingThreadDTO thread : messagePage.getItems()) {
            HomeWorkbenchItemDTO item = new HomeWorkbenchItemDTO();
            item.setId(TYPE_MESSAGE + "-" + thread.getId());
            item.setType(TYPE_MESSAGE);
            item.setSourceType("su_message_thread");
            item.setSourceId(asString(thread.getId()));
            item.setSourceStatus("AWAITING_REPLY");
            item.setStatusGroup(STATUS_AWAITING_REPLY);
            item.setPriority(PRIORITY_MEDIUM);
            item.setDueAt(thread.getLastActivity() == null ? null : thread.getLastActivity().toLocalDateTime());
            item.setTitle(fallback(thread.getGuestName(), "客人") + " 的待回复消息");
            item.setSubtitle(thread.getLastMessage());
            item.setUnreadCount(thread.getUnreadCount());
            addMeta(item, "渠道", thread.getChannelName());
            addMeta(item, "订单", thread.getBookingId());
            addMeta(item, "状态", "待回复");
            if (thread.getUnreadCount() > 0) {
                addMeta(item, "未读", String.valueOf(thread.getUnreadCount()));
            }

            Map<String, String> query = new LinkedHashMap<>();
            query.put("suThreadId", asString(thread.getId()));
            if (thread.getReservationId() != null) {
                query.put("reservationId", asString(thread.getReservationId()));
            }
            item.setTarget(target("su_message_thread", "/messages", query));
            item.setActions(List.of(
                    new HomeWorkbenchActionDTO("view", "查看消息", "default"),
                    new HomeWorkbenchActionDTO("reply", "回复", "primary")
            ));
            items.add(item);
        }
        return items;
    }

    private OtherItems buildOtherItems(boolean manager, int limit) {
        List<InternalTaskDTO> tasks = new ArrayList<>();
        long pendingCount;
        if (manager) {
            InternalTaskPageDTO unassigned = internalTaskService.getManaged(InternalTaskStatus.UNASSIGNED, 0, limit);
            InternalTaskPageDTO assigned = internalTaskService.getManaged(InternalTaskStatus.ASSIGNED, 0, limit);
            tasks.addAll(unassigned.getItems());
            tasks.addAll(assigned.getItems());
            pendingCount = unassigned.getTotalElements() + assigned.getTotalElements();
        } else {
            InternalTaskPageDTO mine = internalTaskService.getMine(InternalTaskStatus.ASSIGNED, 0, limit);
            tasks.addAll(mine.getItems());
            pendingCount = mine.getTotalElements();
        }

        return new OtherItems(tasks.stream().map(this::buildOtherItem).toList(), pendingCount);
    }

    private HomeWorkbenchItemDTO buildOtherItem(InternalTaskDTO task) {
        String statusGroup = task.getStatus().name().toLowerCase(Locale.ROOT);
        HomeWorkbenchItemDTO item = new HomeWorkbenchItemDTO();
        item.setId(TYPE_OTHER + "-" + task.getId());
        item.setType(TYPE_OTHER);
        item.setSourceType("internal_task");
        item.setSourceId(asString(task.getId()));
        item.setSourceStatus(task.getStatus().name());
        item.setStatusGroup(statusGroup);
        item.setPriority(task.getStatus() == InternalTaskStatus.UNASSIGNED ? PRIORITY_HIGH : PRIORITY_MEDIUM);
        item.setDueAt(firstNonNull(task.getUpdatedAt(), task.getCreatedAt()));
        item.setTitle(task.getTitle());
        item.setSubtitle(task.getDescription());
        item.setAssigneeId(task.getAssigneeUserId());
        item.setAssigneeName(task.getAssigneeName());
        addMeta(item, "创建人", task.getCreatedByName());
        addMeta(item, "执行人", task.getAssigneeName());
        addMeta(item, "状态", task.getStatus() == InternalTaskStatus.UNASSIGNED ? "待分配" : "待完成");
        item.setTarget(target(
                "internal_task",
                "/internal-tasks",
                Map.of("taskId", asString(task.getId()))
        ));
        List<HomeWorkbenchActionDTO> actions = new ArrayList<>();
        actions.add(new HomeWorkbenchActionDTO("view", "查看任务", "default"));
        if (task.isCanComplete()) {
            actions.add(new HomeWorkbenchActionDTO("complete", "完成", "primary"));
        }
        item.setActions(actions);
        return item;
    }

    private void appendItems(
            String type,
            List<HomeWorkbenchItemDTO> sourceItems,
            Map<String, Long> typeCounts,
            List<HomeWorkbenchItemDTO> allItems
    ) {
        typeCounts.put(type, (long) sourceItems.size());
        allItems.addAll(sourceItems);
    }

    private List<HomeWorkbenchItemDTO> selectItems(
            String selectedType,
            int limit,
            List<HomeWorkbenchItemDTO> allItems,
            List<HomeWorkbenchItemDTO> cleaningItems,
            List<HomeWorkbenchItemDTO> reviewItems,
            List<HomeWorkbenchItemDTO> orderItems,
            List<HomeWorkbenchItemDTO> messageItems,
            List<HomeWorkbenchItemDTO> otherItems
    ) {
        if (TYPE_CLEANING.equals(selectedType)) {
            return sortAndLimit(cleaningItems, limit);
        }
        if (TYPE_REVIEW.equals(selectedType)) {
            return sortAndLimit(reviewItems, limit);
        }
        if (TYPE_ORDER.equals(selectedType)) {
            return sortAndLimit(orderItems, limit);
        }
        if (TYPE_MESSAGE.equals(selectedType)) {
            return messageItems.stream().limit(limit).toList();
        }
        if (TYPE_OTHER.equals(selectedType)) {
            return sortAndLimit(otherItems, limit);
        }
        return sortAndLimit(allItems, limit);
    }

    private List<HomeWorkbenchItemDTO> sortAndLimit(List<HomeWorkbenchItemDTO> sourceItems, int limit) {
        List<HomeWorkbenchItemDTO> sortedItems = new ArrayList<>(sourceItems);
        sortedItems.sort(this::compareItems);
        return sortedItems.stream().limit(limit).toList();
    }

    private static Map<String, Long> countStatuses(List<HomeWorkbenchItemDTO> items) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (HomeWorkbenchItemDTO item : items) {
            mergeCount(statusCounts, item.getStatusGroup(), 1L);
        }
        return statusCounts;
    }

    private List<HomeWorkbenchTypeSummaryDTO> buildTypeSummaries(
            Map<String, Long> typeCounts,
            Map<String, Boolean> typeAvailability
    ) {
        return List.of(
                new HomeWorkbenchTypeSummaryDTO(TYPE_CLEANING, typeCounts.getOrDefault(TYPE_CLEANING, 0L),
                        typeAvailability.getOrDefault(TYPE_CLEANING, false)),
                new HomeWorkbenchTypeSummaryDTO(TYPE_REVIEW, typeCounts.getOrDefault(TYPE_REVIEW, 0L),
                        typeAvailability.getOrDefault(TYPE_REVIEW, false)),
                new HomeWorkbenchTypeSummaryDTO(TYPE_ORDER, typeCounts.getOrDefault(TYPE_ORDER, 0L),
                        typeAvailability.getOrDefault(TYPE_ORDER, false)),
                new HomeWorkbenchTypeSummaryDTO(TYPE_MESSAGE, typeCounts.getOrDefault(TYPE_MESSAGE, 0L),
                        typeAvailability.getOrDefault(TYPE_MESSAGE, false)),
                new HomeWorkbenchTypeSummaryDTO(TYPE_OTHER, typeCounts.getOrDefault(TYPE_OTHER, 0L),
                        typeAvailability.getOrDefault(TYPE_OTHER, false))
        );
    }

    private List<HomeWorkbenchStatusSummaryDTO> buildStatusSummaries(Map<String, Long> statusCounts) {
        List<HomeWorkbenchStatusSummaryDTO> summaries = new ArrayList<>();
        addStatusSummary(summaries, statusCounts, STATUS_OVERDUE);
        addStatusSummary(summaries, statusCounts, STATUS_PENDING);
        addStatusSummary(summaries, statusCounts, STATUS_AWAITING_REVIEW);
        addStatusSummary(summaries, statusCounts, STATUS_AWAITING_REPLY);
        addStatusSummary(summaries, statusCounts, STATUS_UNASSIGNED);
        addStatusSummary(summaries, statusCounts, STATUS_ASSIGNED);
        addStatusSummary(summaries, statusCounts, STATUS_IN_PROGRESS);
        addStatusSummary(summaries, statusCounts, STATUS_COMPLETED);
        return summaries;
    }

    private boolean hasPermission(
            Long storeId,
            Long userId,
            PermissionModule module,
            PermissionAction action
    ) {
        return permissionService.hasPermission(storeId, userId, module, action);
    }

    private static boolean isManagerContext() {
        String role = StoreContextUtils.requireContext().getRole();
        return "owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    private void addStatusSummary(
            List<HomeWorkbenchStatusSummaryDTO> summaries,
            Map<String, Long> statusCounts,
            String statusGroup
    ) {
        long count = statusCounts.getOrDefault(statusGroup, 0L);
        if (count > 0) {
            summaries.add(new HomeWorkbenchStatusSummaryDTO(statusGroup, count));
        }
    }

    private int compareItems(HomeWorkbenchItemDTO left, HomeWorkbenchItemDTO right) {
        int priorityCompare = Integer.compare(priorityRank(left.getPriority()), priorityRank(right.getPriority()));
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        LocalDateTime leftDueAt = left.getDueAt();
        LocalDateTime rightDueAt = right.getDueAt();
        if (leftDueAt != null && rightDueAt != null) {
            int dueCompare = leftDueAt.compareTo(rightDueAt);
            if (dueCompare != 0) {
                return dueCompare;
            }
        } else if (leftDueAt != null) {
            return -1;
        } else if (rightDueAt != null) {
            return 1;
        }

        return fallback(left.getId(), "").compareTo(fallback(right.getId(), ""));
    }

    private static boolean shouldIncludeCleaningTask(CleaningTaskDTO task) {
        return task != null && !"completed".equalsIgnoreCase(task.getStatus());
    }

    private static String resolveCleaningStatusGroup(String status) {
        if ("expired".equalsIgnoreCase(status)) {
            return STATUS_OVERDUE;
        }
        if ("in_progress".equalsIgnoreCase(status)) {
            return STATUS_IN_PROGRESS;
        }
        return STATUS_PENDING;
    }

    private static String resolveCleaningPriority(String status) {
        if ("expired".equalsIgnoreCase(status)) {
            return PRIORITY_HIGH;
        }
        return PRIORITY_MEDIUM;
    }

    private static LocalDateTime resolveCleaningDueAt(CleaningTaskDTO task) {
        if (task.getEstimatedTime() != null) {
            return task.getEstimatedTime();
        }
        if (task.getTaskDate() != null) {
            return task.getTaskDate().atStartOfDay();
        }
        return null;
    }

    private static List<HomeWorkbenchActionDTO> buildCleaningActions(String status) {
        List<HomeWorkbenchActionDTO> actions = new ArrayList<>();
        actions.add(new HomeWorkbenchActionDTO("view", "查看任务", "default"));
        if ("pending".equalsIgnoreCase(status)) {
            actions.add(new HomeWorkbenchActionDTO("assign_cleaner", "分配保洁", "primary"));
        }
        if ("assigned".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status)) {
            actions.add(new HomeWorkbenchActionDTO("complete", "完成", "primary"));
        }
        return actions;
    }

    private static List<HomeWorkbenchActionDTO> buildOrderActions(String sourceStatus) {
        List<HomeWorkbenchActionDTO> actions = new ArrayList<>();
        actions.add(new HomeWorkbenchActionDTO("view", "查看订单", "default"));
        if ("UNASSIGNED".equals(sourceStatus)) {
            actions.add(new HomeWorkbenchActionDTO("assign_room", "排房", "primary"));
        } else {
            actions.add(new HomeWorkbenchActionDTO("check_in", "办理入住", "primary"));
        }
        return actions;
    }

    private static String resolveOrderPriority(
            Reservation reservation,
            String sourceStatus,
            LocalDate businessDate
    ) {
        if ("UNASSIGNED".equals(sourceStatus)) {
            return PRIORITY_HIGH;
        }
        if (reservation.getCheckInDate() != null && !reservation.getCheckInDate().isAfter(businessDate)) {
            return PRIORITY_HIGH;
        }
        return PRIORITY_MEDIUM;
    }

    private static String resolveOrderTitle(Reservation reservation, String sourceStatus) {
        String guestName = fallback(reservation.getGuestName(), "住客");
        if ("UNASSIGNED".equals(sourceStatus)) {
            return "未排房订单 - " + guestName;
        }
        return "待入住订单 - " + guestName;
    }

    private static String resolveReservationChannelName(Reservation reservation) {
        if (reservation.getChannel() == null) {
            return null;
        }
        return reservation.getChannel().getName();
    }

    private static String resolveReservationRoomNumber(Reservation reservation) {
        Room room = reservation.getRoom();
        if (room != null && room.getRoomNumber() != null && !room.getRoomNumber().isBlank()) {
            return room.getRoomNumber();
        }
        return reservation.getOtaRoomNumber();
    }

    private LocalDate resolveBusinessDate(Long storeId, LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }
        Store store = storeRepository.findById(storeId).orElse(null);
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(store);
        return LocalDate.now(effectiveClock().withZone(zoneId));
    }

    private Clock effectiveClock() {
        return clock == null ? Clock.systemDefaultZone() : clock;
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String normalizeTypeFilter(String type) {
        String normalizedType = blankToNull(type);
        if (normalizedType == null) {
            return TYPE_ALL;
        }

        String lowerCaseType = normalizedType.toLowerCase(Locale.ROOT);
        if (TYPE_ALL.equals(lowerCaseType)) {
            return TYPE_ALL;
        }
        if (TYPE_CLEANING.equals(lowerCaseType)) {
            return TYPE_CLEANING;
        }
        if (TYPE_REVIEW.equals(lowerCaseType)) {
            return TYPE_REVIEW;
        }
        if (TYPE_ORDER.equals(lowerCaseType)) {
            return TYPE_ORDER;
        }
        if (TYPE_MESSAGE.equals(lowerCaseType)) {
            return TYPE_MESSAGE;
        }
        if (TYPE_OTHER.equals(lowerCaseType)) {
            return TYPE_OTHER;
        }
        return TYPE_ALL;
    }

    private static int priorityRank(String priority) {
        if (PRIORITY_HIGH.equals(priority)) {
            return 0;
        }
        if (PRIORITY_MEDIUM.equals(priority)) {
            return 1;
        }
        if (PRIORITY_LOW.equals(priority)) {
            return 2;
        }
        return 3;
    }

    private static void mergeCount(Map<String, Long> counts, String key, long value) {
        if (key == null || value <= 0) {
            return;
        }
        counts.put(key, counts.getOrDefault(key, 0L) + value);
    }

    private static HomeWorkbenchTargetDTO target(String type, String path, Map<String, String> query) {
        return new HomeWorkbenchTargetDTO(type, path, new LinkedHashMap<>(query));
    }

    private static void addMeta(HomeWorkbenchItemDTO item, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        item.getMetaItems().add(new HomeWorkbenchMetaItemDTO(label, value));
    }

    private static String resolveCleaningTaskTypeLabel(String taskType) {
        if ("checkout".equalsIgnoreCase(taskType)) {
            return "退房清洁";
        }
        if ("daily".equalsIgnoreCase(taskType)) {
            return "日常清洁";
        }
        if ("deep".equalsIgnoreCase(taskType)) {
            return "深度清洁";
        }
        return taskType;
    }

    private static String resolveCleaningStatusLabel(String status) {
        if ("pending".equalsIgnoreCase(status)) {
            return "待分配";
        }
        if ("assigned".equalsIgnoreCase(status)) {
            return "待清洁";
        }
        if ("in_progress".equalsIgnoreCase(status)) {
            return "清洁中";
        }
        if ("expired".equalsIgnoreCase(status)) {
            return "已过期";
        }
        return status;
    }

    private static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (Objects.equals(startDate, endDate)) {
            return startDate == null ? null : startDate.toString();
        }
        return fallback(startDate == null ? null : startDate.toString(), "?")
                + " 至 "
                + fallback(endDate == null ? null : endDate.toString(), "?");
    }

    private static String joinNonBlank(String first, String second) {
        String normalizedFirst = blankToNull(first);
        String normalizedSecond = blankToNull(second);
        if (normalizedFirst == null) {
            return normalizedSecond;
        }
        if (normalizedSecond == null) {
            return normalizedFirst;
        }
        return normalizedFirst + " · " + normalizedSecond;
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static String fallback(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record OrderCandidate(Reservation reservation, String sourceStatus) {
    }

    private record ReviewItems(List<HomeWorkbenchItemDTO> items, long awaitingCount) {
    }

    private record OtherItems(List<HomeWorkbenchItemDTO> items, long pendingCount) {
    }
}
