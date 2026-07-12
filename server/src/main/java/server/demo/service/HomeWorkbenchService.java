package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.home.HomeWorkbenchActionDTO;
import server.demo.dto.home.HomeWorkbenchItemDTO;
import server.demo.dto.home.HomeWorkbenchMetaItemDTO;
import server.demo.dto.home.HomeWorkbenchPageDTO;
import server.demo.dto.home.HomeWorkbenchQueryDTO;
import server.demo.dto.home.HomeWorkbenchResponse;
import server.demo.dto.home.HomeWorkbenchStatusSummaryDTO;
import server.demo.dto.home.HomeWorkbenchTargetDTO;
import server.demo.dto.home.HomeWorkbenchTypeSummaryDTO;
import server.demo.dto.internaltask.InternalTaskDTO;
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
    private static final Logger logger = LoggerFactory.getLogger(HomeWorkbenchService.class);
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
    private final HomeWorkbenchCursorCodec cursorCodec = new HomeWorkbenchCursorCodec();

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
        return getWorkbench(requestedDate, requestedLimit, TYPE_ALL, null, null, null, null);
    }

    public HomeWorkbenchResponse getWorkbench(
            LocalDate requestedDate,
            Integer requestedLimit,
            String requestedType
    ) {
        return getWorkbench(requestedDate, requestedLimit, requestedType, null, null, null, null);
    }

    public HomeWorkbenchResponse getWorkbench(
            LocalDate requestedDate,
            Integer requestedLimit,
            String requestedType,
            String requestedStatus,
            Integer requestedSize,
            String requestedCursor,
            Boolean requestedIncludeSummaries
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        if (!storeUserRepository.existsByStoreIdAndUserIdAndIsActiveTrue(storeId, userId)) {
            throw new StoreAccessDeniedException("当前账号未激活或无权访问该门店");
        }
        LocalDate businessDate = resolveBusinessDate(storeId, requestedDate);
        int limit = normalizeLimit(requestedSize == null ? requestedLimit : requestedSize);
        String selectedType = normalizeTypeFilter(requestedType);
        String selectedStatus = normalizeStatusFilter(requestedStatus);
        validateTypeStatus(selectedType, selectedStatus);
        boolean includeSummaries = requestedIncludeSummaries == null
                ? blankToNull(requestedCursor) == null
                : requestedIncludeSummaries;
        String queryContext = buildQueryContext(
                storeId, userId, businessDate, selectedType, selectedStatus, limit, isManagerContext());
        HomeWorkbenchCursorCodec.SortKey cursorKey = cursorCodec.decode(requestedCursor, queryContext);

        List<HomeWorkbenchItemDTO> allItems = new ArrayList<>();
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        Map<String, Boolean> typeAvailability = new LinkedHashMap<>();

        boolean canViewCleaning = hasPermission(
                storeId, userId, PermissionModule.ACCOMMODATION, PermissionAction.TASK_LIST);
        typeAvailability.put(TYPE_CLEANING, canViewCleaning);
        boolean needCleaningItems = TYPE_ALL.equals(selectedType) || TYPE_CLEANING.equals(selectedType);
        CleaningItems cleaningResult = canViewCleaning && (needCleaningItems || includeSummaries)
                ? buildCleaningItems(userId, businessDate, selectedStatus, cursorKey, limit,
                        needCleaningItems, includeSummaries)
                : new CleaningItems(List.of(), 0L, Map.of());
        List<HomeWorkbenchItemDTO> cleaningItems = cleaningResult.items();
        typeCounts.put(TYPE_CLEANING, cleaningResult.pendingCount());
        allItems.addAll(cleaningItems);

        boolean canViewReview = hasPermission(
                storeId, userId, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS);
        typeAvailability.put(TYPE_REVIEW, canViewReview);
        boolean needReviewItems = TYPE_ALL.equals(selectedType) || TYPE_REVIEW.equals(selectedType);
        ReviewItems reviewResult = canViewReview && (needReviewItems || includeSummaries)
                ? buildReviewItems(selectedStatus, cursorKey, limit, needReviewItems, includeSummaries)
                : new ReviewItems(List.of(), 0L, 0L);
        List<HomeWorkbenchItemDTO> reviewItems = reviewResult.items();
        typeCounts.put(TYPE_REVIEW, reviewResult.awaitingCount() + reviewResult.completedCount());
        allItems.addAll(reviewItems);

        boolean canViewOrder = hasPermission(
                storeId, userId, PermissionModule.ORDER, PermissionAction.VIEW_ORDERS);
        typeAvailability.put(TYPE_ORDER, canViewOrder);
        boolean needOrderItems = TYPE_ALL.equals(selectedType) || TYPE_ORDER.equals(selectedType);
        OrderItems orderResult = canViewOrder && (needOrderItems || includeSummaries)
                ? buildOrderItems(storeId, businessDate, selectedStatus, cursorKey, limit,
                        needOrderItems, includeSummaries)
                : new OrderItems(List.of(), 0L);
        List<HomeWorkbenchItemDTO> orderItems = orderResult.items();
        typeCounts.put(TYPE_ORDER, orderResult.pendingCount());
        allItems.addAll(orderItems);

        List<HomeWorkbenchItemDTO> messageItems = List.of();
        long awaitingReplyCount = 0L;
        boolean messageAvailable = true;
        try {
            if (TYPE_ALL.equals(selectedType) || TYPE_MESSAGE.equals(selectedType)) {
                HomeWorkbenchCursorCodec.SortKey messageCursor = cursorForType(cursorKey, TYPE_MESSAGE);
                SuMessagingThreadPageResponse messagePage = suMessagingService.listAwaitingReplyThreadSlice(
                        storeId,
                        messageCursor == null ? null : messageCursor.dueAt(),
                        messageCursor == null ? null : messageCursor.sourceId(),
                        limit
                );
                messageItems = buildMessageItems(messagePage);
            }
            if (includeSummaries) {
                awaitingReplyCount = suMessagingService.countAwaitingReplyThreads(storeId);
            }
        } catch (RuntimeException exception) {
            messageAvailable = false;
            logger.warn(
                    "[HomeWorkbench] awaiting-reply section degraded. storeId={}, selectedType={}, error={}",
                    storeId,
                    selectedType,
                    exception.getMessage()
            );
        }
        typeCounts.put(TYPE_MESSAGE, awaitingReplyCount);
        typeAvailability.put(TYPE_MESSAGE, messageAvailable);
        allItems.addAll(messageItems);

        OtherItems otherResult = (TYPE_ALL.equals(selectedType) || TYPE_OTHER.equals(selectedType) || includeSummaries)
                ? buildOtherItems(selectedStatus, cursorKey, limit,
                        TYPE_ALL.equals(selectedType) || TYPE_OTHER.equals(selectedType), includeSummaries)
                : new OtherItems(List.of(), 0L, 0L, 0L, 0L);
        List<HomeWorkbenchItemDTO> otherItems = otherResult.items();
        typeCounts.put(TYPE_OTHER, otherResult.pendingCount() + otherResult.completedCount());
        typeAvailability.put(TYPE_OTHER, true);
        allItems.addAll(otherItems);

        List<HomeWorkbenchItemDTO> selectedSourceItems = selectSourceItems(
                selectedType, allItems, cleaningItems, reviewItems, orderItems, messageItems, otherItems);
        List<HomeWorkbenchItemDTO> filteredItems = filterItems(selectedSourceItems, selectedStatus, cursorKey);
        boolean hasMore = filteredItems.size() > limit;
        List<HomeWorkbenchItemDTO> selectedItems = filteredItems.stream().limit(limit).toList();
        String nextCursor = hasMore && !selectedItems.isEmpty()
                ? cursorCodec.encode(queryContext, sortKey(selectedItems.get(selectedItems.size() - 1)))
                : null;

        List<HomeWorkbenchItemDTO> statusSourceItems = selectSourceItems(
                selectedType, allItems, cleaningItems, reviewItems, orderItems, List.of(), otherItems);
        if (TYPE_ALL.equals(selectedType)) {
            statusSourceItems = statusSourceItems.stream()
                    .filter(item -> !TYPE_MESSAGE.equals(item.getType()) && !TYPE_REVIEW.equals(item.getType())
                            && !TYPE_OTHER.equals(item.getType()) && !TYPE_CLEANING.equals(item.getType()))
                    .filter(item -> !TYPE_ORDER.equals(item.getType()))
                    .toList();
        } else if (TYPE_REVIEW.equals(selectedType) || TYPE_CLEANING.equals(selectedType)
                || TYPE_ORDER.equals(selectedType)) {
            statusSourceItems = List.of();
        } else if (TYPE_OTHER.equals(selectedType)) {
            statusSourceItems = List.of();
        }
        Map<String, Long> statusCounts = countStatuses(statusSourceItems);
        if (TYPE_ALL.equals(selectedType) || TYPE_CLEANING.equals(selectedType)) {
            for (Map.Entry<String, Long> entry : cleaningResult.statusCounts().entrySet()) {
                mergeCount(statusCounts, entry.getKey(), entry.getValue());
            }
        }
        if (TYPE_ALL.equals(selectedType) || TYPE_ORDER.equals(selectedType)) {
            mergeCount(statusCounts, STATUS_PENDING, orderResult.pendingCount());
        }
        if ((TYPE_ALL.equals(selectedType) || TYPE_REVIEW.equals(selectedType)) && canViewReview) {
            mergeCount(statusCounts, STATUS_AWAITING_REVIEW, reviewResult.awaitingCount());
            mergeCount(statusCounts, STATUS_COMPLETED, reviewResult.completedCount());
        }
        if (TYPE_ALL.equals(selectedType) || TYPE_OTHER.equals(selectedType)) {
            mergeCount(statusCounts, STATUS_UNASSIGNED, otherResult.unassignedCount());
            mergeCount(statusCounts, STATUS_ASSIGNED, otherResult.assignedCount());
            mergeCount(statusCounts, STATUS_COMPLETED, otherResult.completedCount());
        }
        if ((TYPE_ALL.equals(selectedType) || TYPE_MESSAGE.equals(selectedType)) && messageAvailable) {
            mergeCount(statusCounts, STATUS_AWAITING_REPLY, awaitingReplyCount);
        }
        Long totalElements = null;
        if (includeSummaries) {
            totalElements = selectedStatus == null
                    ? statusCounts.values().stream().mapToLong(Long::longValue).sum()
                    : statusCounts.getOrDefault(selectedStatus, 0L);
        }

        HomeWorkbenchResponse response = new HomeWorkbenchResponse();
        response.setBusinessDate(businessDate);
        response.setGeneratedAt(LocalDateTime.now(effectiveClock()));
        response.setQuery(new HomeWorkbenchQueryDTO(selectedType, selectedStatus, limit, "priority_due"));
        response.setTypeSummaries(includeSummaries
                ? buildTypeSummaries(typeCounts, typeAvailability) : null);
        response.setStatusSummaries(includeSummaries ? buildStatusSummaries(statusCounts) : null);
        response.setPage(new HomeWorkbenchPageDTO(
                limit, selectedItems.size(), totalElements, hasMore, nextCursor));
        response.setItems(selectedItems);
        return response;
    }

    private CleaningItems buildCleaningItems(Long userId, LocalDate businessDate, String selectedStatus,
                                              HomeWorkbenchCursorCodec.SortKey cursor, int limit,
                                              boolean needItems, boolean includeSummaries) {
        List<CleaningTaskDTO> tasks = List.of();
        if (needItems) {
            HomeWorkbenchCursorCodec.SortKey sourceCursor = cursorForVariableType(cursor, TYPE_CLEANING);
            tasks = cleaningTaskService.getHomeTaskSlice(userId, businessDate, selectedStatus,
                    sourceCursor == null ? null : sourceCursor.priorityRank(),
                    sourceCursor == null ? null : sourceCursor.dueAt(),
                    sourceCursor == null ? null : sourceCursor.sourceId(), limit + 1);
        }
        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (CleaningTaskDTO task : tasks) {
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
        Map<String, Long> groupedCounts = new LinkedHashMap<>();
        if (includeSummaries) {
            for (Map.Entry<String, Long> entry : cleaningTaskService
                    .getHomeTaskStatusCounts(userId, businessDate).entrySet()) {
                mergeCount(groupedCounts, resolveCleaningStatusGroup(entry.getKey()), entry.getValue());
            }
        }
        long count = groupedCounts.values().stream().mapToLong(Long::longValue).sum();
        return new CleaningItems(items, count, groupedCounts);
    }

    private ReviewItems buildReviewItems(
            String selectedStatus,
            HomeWorkbenchCursorCodec.SortKey cursor,
            int limit,
            boolean needItems,
            boolean includeSummaries
    ) {
        LocalDateTime completedSince = LocalDateTime.now(effectiveClock()).minusDays(COMPLETED_REVIEW_RETENTION_DAYS);
        RegistrationFormStatus status = null;
        if (STATUS_AWAITING_REVIEW.equals(selectedStatus)) status = RegistrationFormStatus.SUBMITTED;
        if (STATUS_COMPLETED.equals(selectedStatus)) status = RegistrationFormStatus.APPROVED;

        List<AdminRegistrationListItemDTO> forms = List.of();
        if (needItems) {
            HomeWorkbenchCursorCodec.SortKey sourceCursor = cursorForVariableType(cursor, TYPE_REVIEW);
            forms = registrationAdminService.listHomeSlice(
                    completedSince,
                    status,
                    sourceCursor == null ? null : sourceCursor.priorityRank(),
                    sourceCursor == null ? null : sourceCursor.dueAt(),
                    sourceCursor == null ? null : sourceCursor.sourceId(),
                    limit + 1
            );
        }

        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (AdminRegistrationListItemDTO form : forms) {
            items.add(buildReviewItem(form, form.getStatus() == RegistrationFormStatus.APPROVED));
        }
        long awaitingCount = includeSummaries
                ? registrationAdminService.countHome(RegistrationFormStatus.SUBMITTED, null) : 0L;
        long completedCount = includeSummaries
                ? registrationAdminService.countHome(RegistrationFormStatus.APPROVED, completedSince) : 0L;
        return new ReviewItems(items, awaitingCount, completedCount);
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

    private OrderItems buildOrderItems(Long storeId, LocalDate businessDate, String selectedStatus,
                                       HomeWorkbenchCursorCodec.SortKey cursor, int limit,
                                       boolean needItems, boolean includeSummaries) {
        List<Reservation> reservations = List.of();
        if (needItems && (selectedStatus == null || STATUS_PENDING.equals(selectedStatus))) {
            HomeWorkbenchCursorCodec.SortKey sourceCursor = cursorForVariableType(cursor, TYPE_ORDER);
            LocalDate cursorDate = sourceCursor == null || sourceCursor.dueAt() == null
                    ? businessDate : sourceCursor.dueAt().toLocalDate();
            reservations = reservationRepository.findHomeOrderSlice(storeId, businessDate,
                    sourceCursor != null,
                    sourceCursor == null ? 0 : sourceCursor.priorityRank(),
                    sourceCursor == null ? 0 : sourceCursor.dueAtNullRank(),
                    cursorDate,
                    sourceCursor == null ? 0L : sourceCursor.sourceId(),
                    PageRequest.of(0, Math.min(limit + 1, 101)));
        }
        List<HomeWorkbenchItemDTO> items = new ArrayList<>();
        for (Reservation reservation : reservations) {
            String sourceStatus = reservation.getRoom() == null ? "UNASSIGNED" : "PENDING";
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
        long count = includeSummaries ? reservationRepository.countHomeOrders(storeId, businessDate) : 0L;
        return new OrderItems(items, count);
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

    private OtherItems buildOtherItems(String selectedStatus, HomeWorkbenchCursorCodec.SortKey cursor,
                                       int limit, boolean needItems, boolean includeSummaries) {
        LocalDateTime completedSince = LocalDateTime.now(effectiveClock())
                .minusDays(COMPLETED_REVIEW_RETENTION_DAYS);
        InternalTaskStatus status = null;
        if (STATUS_UNASSIGNED.equals(selectedStatus)) status = InternalTaskStatus.UNASSIGNED;
        if (STATUS_ASSIGNED.equals(selectedStatus)) status = InternalTaskStatus.ASSIGNED;
        if (STATUS_COMPLETED.equals(selectedStatus)) status = InternalTaskStatus.COMPLETED;
        List<InternalTaskDTO> tasks = List.of();
        if (needItems) {
            HomeWorkbenchCursorCodec.SortKey sourceCursor = cursorForVariableType(cursor, TYPE_OTHER);
            tasks = internalTaskService.listHomeSlice(status, completedSince,
                    sourceCursor == null ? null : sourceCursor.priorityRank(),
                    sourceCursor == null ? null : sourceCursor.dueAt(),
                    sourceCursor == null ? null : sourceCursor.sourceId(), limit + 1);
        }
        long unassigned = includeSummaries ? internalTaskService.countHome(InternalTaskStatus.UNASSIGNED, completedSince) : 0;
        long assigned = includeSummaries ? internalTaskService.countHome(InternalTaskStatus.ASSIGNED, completedSince) : 0;
        long completed = includeSummaries ? internalTaskService.countHome(InternalTaskStatus.COMPLETED, completedSince) : 0;
        return new OtherItems(tasks.stream().map(this::buildOtherItem).toList(),
                unassigned + assigned, unassigned, assigned, completed);
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
        if (task.getStatus() == InternalTaskStatus.UNASSIGNED) {
            item.setPriority(PRIORITY_HIGH);
        } else if (task.getStatus() == InternalTaskStatus.COMPLETED) {
            item.setPriority(PRIORITY_LOW);
        } else {
            item.setPriority(PRIORITY_MEDIUM);
        }
        item.setDueAt(task.getStatus() == InternalTaskStatus.COMPLETED
                ? firstNonNull(task.getCompletedAt(), task.getUpdatedAt())
                : firstNonNull(task.getUpdatedAt(), task.getCreatedAt()));
        item.setTitle(task.getTitle());
        item.setSubtitle(task.getDescription());
        item.setAssigneeId(task.getAssigneeUserId());
        item.setAssigneeName(task.getAssigneeName());
        addMeta(item, "创建人", task.getCreatedByName());
        addMeta(item, "执行人", task.getAssigneeName());
        addMeta(item, "状态", resolveInternalTaskStatusLabel(task.getStatus()));
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

    private List<HomeWorkbenchItemDTO> selectSourceItems(
            String selectedType,
            List<HomeWorkbenchItemDTO> allItems,
            List<HomeWorkbenchItemDTO> cleaningItems,
            List<HomeWorkbenchItemDTO> reviewItems,
            List<HomeWorkbenchItemDTO> orderItems,
            List<HomeWorkbenchItemDTO> messageItems,
            List<HomeWorkbenchItemDTO> otherItems
    ) {
        if (TYPE_CLEANING.equals(selectedType)) {
            return cleaningItems;
        }
        if (TYPE_REVIEW.equals(selectedType)) {
            return reviewItems;
        }
        if (TYPE_ORDER.equals(selectedType)) {
            return orderItems;
        }
        if (TYPE_MESSAGE.equals(selectedType)) {
            return messageItems;
        }
        if (TYPE_OTHER.equals(selectedType)) {
            return otherItems;
        }
        return allItems;
    }

    private List<HomeWorkbenchItemDTO> filterItems(
            List<HomeWorkbenchItemDTO> sourceItems,
            String selectedStatus,
            HomeWorkbenchCursorCodec.SortKey cursorKey
    ) {
        List<HomeWorkbenchItemDTO> sortedItems = sourceItems.stream()
                .filter(item -> selectedStatus == null || selectedStatus.equals(item.getStatusGroup()))
                .filter(item -> cursorKey == null || compareSortKeys(sortKey(item), cursorKey) > 0)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        sortedItems.sort(this::compareItems);
        return sortedItems;
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
        return compareSortKeys(sortKey(left), sortKey(right));
    }

    private HomeWorkbenchCursorCodec.SortKey sortKey(HomeWorkbenchItemDTO item) {
        return new HomeWorkbenchCursorCodec.SortKey(
                priorityRank(item.getPriority()),
                item.getDueAt() == null ? 1 : 0,
                item.getDueAt(),
                typeRank(item.getType()),
                parseSourceId(item.getSourceId())
        );
    }

    private int compareSortKeys(
            HomeWorkbenchCursorCodec.SortKey left,
            HomeWorkbenchCursorCodec.SortKey right
    ) {
        int compared = Integer.compare(left.priorityRank(), right.priorityRank());
        if (compared != 0) return compared;
        compared = Integer.compare(left.dueAtNullRank(), right.dueAtNullRank());
        if (compared != 0) return compared;
        if (left.dueAt() != null && right.dueAt() != null) {
            compared = left.dueAt().compareTo(right.dueAt());
            if (compared != 0) return compared;
        }
        compared = Integer.compare(left.typeRank(), right.typeRank());
        if (compared != 0) return compared;
        return Long.compare(left.sourceId(), right.sourceId());
    }

    private HomeWorkbenchCursorCodec.SortKey cursorForType(
            HomeWorkbenchCursorCodec.SortKey cursor,
            String type
    ) {
        if (cursor == null) return null;
        int sourcePriority = TYPE_MESSAGE.equals(type) ? priorityRank(PRIORITY_MEDIUM) : cursor.priorityRank();
        if (cursor.priorityRank() < sourcePriority) return null;
        if (cursor.priorityRank() > sourcePriority) {
            return new HomeWorkbenchCursorCodec.SortKey(
                    cursor.priorityRank(), cursor.dueAtNullRank(), cursor.dueAt(), typeRank(type), Long.MAX_VALUE);
        }
        long sourceId = cursor.sourceId();
        int sourceTypeRank = typeRank(type);
        if (cursor.typeRank() < sourceTypeRank) sourceId = 0L;
        if (cursor.typeRank() > sourceTypeRank) sourceId = Long.MAX_VALUE;
        return new HomeWorkbenchCursorCodec.SortKey(
                cursor.priorityRank(), cursor.dueAtNullRank(), cursor.dueAt(), sourceTypeRank, sourceId);
    }

    private HomeWorkbenchCursorCodec.SortKey cursorForVariableType(
            HomeWorkbenchCursorCodec.SortKey cursor,
            String type
    ) {
        if (cursor == null) return null;
        long sourceId = cursor.sourceId();
        int sourceTypeRank = typeRank(type);
        if (cursor.typeRank() < sourceTypeRank) sourceId = 0L;
        if (cursor.typeRank() > sourceTypeRank) sourceId = Long.MAX_VALUE;
        return new HomeWorkbenchCursorCodec.SortKey(
                cursor.priorityRank(), cursor.dueAtNullRank(), cursor.dueAt(), sourceTypeRank, sourceId);
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
        throw new IllegalArgumentException("不支持的工作台类型: " + normalizedType);
    }

    private static String normalizeStatusFilter(String status) {
        String normalizedStatus = blankToNull(status);
        if (normalizedStatus == null) return null;
        String value = normalizedStatus.toLowerCase(Locale.ROOT);
        if (STATUS_PENDING.equals(value)
                || STATUS_AWAITING_REVIEW.equals(value)
                || STATUS_AWAITING_REPLY.equals(value)
                || STATUS_UNASSIGNED.equals(value)
                || STATUS_ASSIGNED.equals(value)
                || STATUS_IN_PROGRESS.equals(value)
                || STATUS_OVERDUE.equals(value)
                || STATUS_COMPLETED.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("不支持的工作台状态: " + normalizedStatus);
    }

    private static void validateTypeStatus(String type, String status) {
        if (status == null || TYPE_ALL.equals(type)) return;
        boolean valid = switch (type) {
            case TYPE_CLEANING -> STATUS_PENDING.equals(status)
                    || STATUS_IN_PROGRESS.equals(status) || STATUS_OVERDUE.equals(status);
            case TYPE_REVIEW -> STATUS_AWAITING_REVIEW.equals(status) || STATUS_COMPLETED.equals(status);
            case TYPE_ORDER -> STATUS_PENDING.equals(status);
            case TYPE_MESSAGE -> STATUS_AWAITING_REPLY.equals(status);
            case TYPE_OTHER -> STATUS_UNASSIGNED.equals(status)
                    || STATUS_ASSIGNED.equals(status) || STATUS_COMPLETED.equals(status);
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("工作台类型与状态不匹配");
        }
    }

    private static String buildQueryContext(
            Long storeId,
            Long userId,
            LocalDate businessDate,
            String type,
            String status,
            int size,
            boolean manager
    ) {
        return storeId + ":" + userId + ":" + businessDate + ":" + type + ":"
                + fallback(status, "*") + ":" + size + ":priority_due:"
                + (manager ? "manager" : "member");
    }

    private static int typeRank(String type) {
        if (TYPE_CLEANING.equals(type)) return 1;
        if (TYPE_REVIEW.equals(type)) return 2;
        if (TYPE_ORDER.equals(type)) return 3;
        if (TYPE_MESSAGE.equals(type)) return 4;
        if (TYPE_OTHER.equals(type)) return 5;
        return 9;
    }

    private static long parseSourceId(String sourceId) {
        try {
            return Long.parseLong(sourceId);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("工作台数据缺少稳定数值主键: " + sourceId, exception);
        }
    }

    private static String resolveInternalTaskStatusLabel(InternalTaskStatus status) {
        if (status == InternalTaskStatus.UNASSIGNED) return "待分配";
        if (status == InternalTaskStatus.COMPLETED) return "已完成";
        return "待完成";
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

    private record ReviewItems(List<HomeWorkbenchItemDTO> items, long awaitingCount, long completedCount) {
    }

    private record CleaningItems(List<HomeWorkbenchItemDTO> items, long pendingCount,
                                 Map<String, Long> statusCounts) {
    }

    private record OrderItems(List<HomeWorkbenchItemDTO> items, long pendingCount) {
    }

    private record OtherItems(List<HomeWorkbenchItemDTO> items, long pendingCount,
                              long unassignedCount, long assignedCount, long completedCount) {
    }
}
