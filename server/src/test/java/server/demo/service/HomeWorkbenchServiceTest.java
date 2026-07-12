package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.home.HomeWorkbenchItemDTO;
import server.demo.dto.home.HomeWorkbenchResponse;
import server.demo.dto.internaltask.InternalTaskDTO;
import server.demo.entity.Channel;
import server.demo.entity.InternalTask;
import server.demo.entity.Reservation;
import server.demo.enums.InternalTaskStatus;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;
import server.demo.exception.StoreAccessDeniedException;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class HomeWorkbenchServiceTest {
    private static final long USER_ID = 3L;
    private static final long STORE_ID = 8L;

    private CleaningTaskService cleaningTaskService;
    private RegistrationAdminService registrationAdminService;
    private ReservationRepository reservationRepository;
    private SuMessagingService suMessagingService;
    private StoreRepository storeRepository;
    private StoreUserRepository storeUserRepository;
    private PermissionService permissionService;
    private InternalTaskService internalTaskService;
    private HomeWorkbenchService service;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        cleaningTaskService = Mockito.mock(CleaningTaskService.class);
        registrationAdminService = Mockito.mock(RegistrationAdminService.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        suMessagingService = Mockito.mock(SuMessagingService.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        storeUserRepository = Mockito.mock(StoreUserRepository.class);
        permissionService = Mockito.mock(PermissionService.class);
        internalTaskService = Mockito.mock(InternalTaskService.class);

        service = new HomeWorkbenchService(
                cleaningTaskService,
                registrationAdminService,
                reservationRepository,
                suMessagingService,
                storeRepository,
                storeUserRepository,
                permissionService,
                internalTaskService,
                Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC)
        );

        when(storeUserRepository.existsByStoreIdAndUserIdAndIsActiveTrue(STORE_ID, USER_ID)).thenReturn(true);
        when(permissionService.hasPermission(anyLong(), anyLong(), any(), any())).thenReturn(true);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.empty());
        when(cleaningTaskService.getHomeTaskSlice(anyLong(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(cleaningTaskService.getHomeTaskStatusCounts(anyLong(), any())).thenReturn(java.util.Map.of());
        when(registrationAdminService.list(
                eq(RegistrationFormStatus.SUBMITTED),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(List.of());
        when(registrationAdminService.listRecentApprovedForHome(any())).thenReturn(List.of());
        when(registrationAdminService.listHomeSlice(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(registrationAdminService.countHome(any(), any())).thenReturn(0L);
        when(suMessagingService.listAwaitingReplyThreadSlice(
                eq(STORE_ID), isNull(), isNull(), anyInt()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(), 0, 50, 0, 0, false));
        when(reservationRepository.findHomeOrderSlice(eq(STORE_ID), any(), anyBoolean(), anyInt(),
                anyInt(), any(), anyLong(), any())).thenReturn(List.of());
        when(reservationRepository.countHomeOrders(eq(STORE_ID), any())).thenReturn(0L);
        when(internalTaskService.listHomeSlice(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(internalTaskService.countHome(any(), any())).thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getWorkbench_deduplicatesOrderCandidatesAndKeepsUnassignedPriority() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        Reservation reservation = buildReservation(10L, "RSV-10", businessDate);

        when(reservationRepository.findHomeOrderSlice(eq(STORE_ID), eq(businessDate), eq(false), anyInt(),
                anyInt(), any(), anyLong(), any())).thenReturn(List.of(reservation));
        when(reservationRepository.countHomeOrders(STORE_ID, businessDate)).thenReturn(1L);

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 50);

        assertEquals(1, response.getItems().size());
        HomeWorkbenchItemDTO item = response.getItems().get(0);
        assertEquals("order", item.getType());
        assertEquals("UNASSIGNED", item.getSourceStatus());
        assertEquals("high", item.getPriority());
        assertEquals("pending", item.getStatusGroup());
        assertEquals("/order", item.getTarget().getPath());
        assertEquals("unassigned", item.getTarget().getQuery().get("type"));
        assertEquals("assign_room", item.getActions().get(1).getCode());
        assertEquals(1L, response.getTypeSummaries().get(2).getCount());
        Mockito.verify(reservationRepository).findHomeOrderSlice(eq(STORE_ID), eq(businessDate), eq(false),
                anyInt(), anyInt(), any(), anyLong(), any());
    }

    @Test
    void getWorkbench_usesUnreadThreadTotalForMessageSummary() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        SuMessagingThreadDTO thread = new SuMessagingThreadDTO();
        thread.setId(31L);
        thread.setGuestName("Yamada");
        thread.setChannelName("Booking.com");
        thread.setBookingId("BK-31");
        thread.setLastMessage("Arrival time?");
        thread.setLastActivity(OffsetDateTime.parse("2026-06-12T01:00:00Z"));
        thread.setUnreadCount(3L);
        when(suMessagingService.listAwaitingReplyThreadSlice(
                eq(STORE_ID), isNull(), isNull(), anyInt()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(thread), 0, 5, 0, 0, true));
        when(suMessagingService.countAwaitingReplyThreads(STORE_ID)).thenReturn(7L);

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 5);

        assertEquals(1, response.getItems().size());
        assertEquals("message", response.getItems().get(0).getType());
        assertEquals(3L, response.getItems().get(0).getUnreadCount());
        assertEquals("awaiting_reply", response.getItems().get(0).getStatusGroup());
        assertEquals("AWAITING_REPLY", response.getItems().get(0).getSourceStatus());
        assertEquals("31", response.getItems().get(0).getTarget().getQuery().get("suThreadId"));
        assertFalse(response.getItems().get(0).getTarget().getQuery().containsKey("threadId"));
        assertEquals(7L, response.getTypeSummaries().get(3).getCount());
        assertEquals("awaiting_reply", response.getStatusSummaries().get(0).getStatusGroup());
        assertEquals(7L, response.getStatusSummaries().get(0).getCount());
    }

    @Test
    void getWorkbench_withMessageTypeReturnsMessageItemsOutsideGlobalLimit() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        Reservation reservation = buildReservation(10L, "RSV-10", businessDate);

        when(reservationRepository.findHomeOrderSlice(eq(STORE_ID), eq(businessDate), eq(false), anyInt(),
                anyInt(), any(), anyLong(), any())).thenReturn(List.of(reservation));
        when(reservationRepository.countHomeOrders(STORE_ID, businessDate)).thenReturn(1L);

        SuMessagingThreadDTO thread = buildMessageThread(31L, "Yamada", "BK-31");
        when(suMessagingService.listAwaitingReplyThreadSlice(
                eq(STORE_ID), isNull(), isNull(), anyInt()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(thread), 0, 1, 0, 0, true));
        when(suMessagingService.countAwaitingReplyThreads(STORE_ID)).thenReturn(16L);

        HomeWorkbenchResponse allResponse = service.getWorkbench(businessDate, 1);
        assertEquals(1, allResponse.getItems().size());
        assertEquals("order", allResponse.getItems().get(0).getType());
        assertEquals(16L, allResponse.getTypeSummaries().get(3).getCount());

        HomeWorkbenchResponse messageResponse = service.getWorkbench(businessDate, 1, "message");
        assertEquals(1, messageResponse.getItems().size());
        assertEquals("message", messageResponse.getItems().get(0).getType());
        assertEquals("31", messageResponse.getItems().get(0).getTarget().getQuery().get("suThreadId"));
        assertEquals(16L, messageResponse.getTypeSummaries().get(3).getCount());
        assertEquals("awaiting_reply", messageResponse.getStatusSummaries().get(0).getStatusGroup());
        assertEquals(16L, messageResponse.getStatusSummaries().get(0).getCount());
    }

    @Test
    void getWorkbench_withNonMessageTypeUsesCountWithoutHydratingMessageDtos() {
        when(suMessagingService.countAwaitingReplyThreads(STORE_ID)).thenReturn(12L);

        HomeWorkbenchResponse response = service.getWorkbench(
                LocalDate.of(2026, 6, 12),
                20,
                "other"
        );

        assertEquals(12L, response.getTypeSummaries().get(3).getCount());
        Mockito.verify(suMessagingService).countAwaitingReplyThreads(STORE_ID);
        Mockito.verify(suMessagingService, never())
                .listAwaitingReplyThreadPage(anyLong(), anyInt(), anyInt());
        Mockito.verify(suMessagingService, never())
                .listAwaitingReplyThreadSlice(anyLong(), any(), any(), anyInt());
    }

    @Test
    void getWorkbench_messageCountFailureDegradesWithoutFailingOtherType() {
        when(suMessagingService.countAwaitingReplyThreads(STORE_ID))
                .thenThrow(new RuntimeException("query timeout"));

        HomeWorkbenchResponse response = service.getWorkbench(
                LocalDate.of(2026, 6, 12),
                20,
                "other"
        );

        assertTrue(response.getItems().isEmpty());
        assertEquals(0L, response.getTypeSummaries().get(3).getCount());
        assertFalse(response.getTypeSummaries().get(3).isConnected());
    }

    @Test
    void getWorkbench_outputsFrontendActionContractForCleaningAssignment() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        CleaningTaskDTO task = new CleaningTaskDTO();
        task.setId(51L);
        task.setTaskDate(businessDate);
        task.setTaskType("checkout");
        task.setStatus("pending");
        task.setRoomNumber("201");
        task.setRoomType("Double");

        when(cleaningTaskService.getHomeTaskSlice(anyLong(), eq(businessDate), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(task));
        when(cleaningTaskService.getHomeTaskStatusCounts(anyLong(), eq(businessDate)))
                .thenReturn(java.util.Map.of("pending", 1L));

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 50);

        HomeWorkbenchItemDTO item = response.getItems().get(0);
        assertEquals("cleaning", item.getType());
        assertEquals("pending", item.getStatusGroup());
        assertEquals("assign_cleaner", item.getActions().get(1).getCode());
        assertEquals("primary", item.getActions().get(1).getType());

        JsonNode actionJson = new ObjectMapper().valueToTree(item.getActions().get(1));
        assertTrue(actionJson.has("code"));
        assertTrue(actionJson.has("type"));
        assertFalse(actionJson.has("key"));
        assertFalse(actionJson.has("style"));
    }

    @Test
    void getWorkbench_cleaningCursorCoversFiftyOneWithSameDueAtIdTieBreak() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        List<CleaningTaskDTO> tasks = new ArrayList<>();
        for (long id = 1; id <= 51; id++) {
            CleaningTaskDTO task = new CleaningTaskDTO(); task.setId(id); task.setTaskDate(date);
            task.setStatus("pending"); task.setTaskType("daily"); task.setRoomNumber("R" + id);
            task.setEstimatedTime(LocalDateTime.of(2026, 6, 12, 9, 0)); tasks.add(task);
        }
        when(cleaningTaskService.getHomeTaskSlice(anyLong(), eq(date), eq("pending"),
                any(), any(), any(), anyInt())).thenReturn(tasks);
        when(cleaningTaskService.getHomeTaskStatusCounts(anyLong(), eq(date)))
                .thenReturn(java.util.Map.of("pending", 51L));

        HomeWorkbenchResponse first = service.getWorkbench(date, null, "cleaning", "pending", 50, null, true);
        HomeWorkbenchResponse second = service.getWorkbench(date, null, "cleaning", "pending", 50,
                first.getPage().getNextCursor(), false);

        assertEquals(51L, first.getPage().getTotalElements());
        assertEquals(50, first.getItems().size()); assertTrue(first.getPage().isHasMore());
        assertEquals("51", second.getItems().get(0).getSourceId()); assertFalse(second.getPage().isHasMore());
        Mockito.verify(cleaningTaskService, Mockito.times(1)).getHomeTaskStatusCounts(USER_ID, date);
    }

    @Test
    void getWorkbench_orderCursorUsesCheckInThenIdAcrossOneHundredAndOne() {
        LocalDate date = LocalDate.of(2026, 6, 12); List<Reservation> reservations = new ArrayList<>();
        for (long id = 1; id <= 101; id++) reservations.add(buildReservation(id, "RSV-" + id, date));
        when(reservationRepository.findHomeOrderSlice(eq(STORE_ID), eq(date), anyBoolean(), anyInt(),
                anyInt(), any(), anyLong(), any())).thenReturn(reservations);
        when(reservationRepository.countHomeOrders(STORE_ID, date)).thenReturn(101L);

        HomeWorkbenchResponse first = service.getWorkbench(date, null, "order", "pending", 50, null, true);
        HomeWorkbenchResponse second = service.getWorkbench(date, null, "order", "pending", 50,
                first.getPage().getNextCursor(), false);
        HomeWorkbenchResponse third = service.getWorkbench(date, null, "order", "pending", 50,
                second.getPage().getNextCursor(), false);

        assertEquals(101L, first.getPage().getTotalElements()); assertTrue(first.getPage().isHasMore());
        assertEquals("51", second.getItems().get(0).getSourceId()); assertTrue(second.getPage().isHasMore());
        assertEquals("101", third.getItems().get(0).getSourceId()); assertFalse(third.getPage().isHasMore());
        Mockito.verify(reservationRepository, Mockito.times(1)).countHomeOrders(STORE_ID, date);
    }

    @Test
    void getWorkbench_allCursorUsesTypeRankWhenPriorityAndDueAreEqual() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        CleaningTaskDTO cleaning = new CleaningTaskDTO(); cleaning.setId(9L); cleaning.setTaskDate(date);
        cleaning.setStatus("expired"); cleaning.setTaskType("daily"); cleaning.setRoomNumber("201");
        cleaning.setEstimatedTime(LocalDateTime.of(2026, 6, 12, 10, 0));
        when(cleaningTaskService.getHomeTaskSlice(anyLong(), eq(date), isNull(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(cleaning));
        server.demo.dto.registration.AdminRegistrationListItemDTO review =
                new server.demo.dto.registration.AdminRegistrationListItemDTO();
        review.setFormId(9L); review.setStatus(RegistrationFormStatus.SUBMITTED);
        review.setSubmittedAt(LocalDateTime.of(2026, 6, 12, 10, 0));
        when(registrationAdminService.listHomeSlice(any(), isNull(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(review));

        HomeWorkbenchResponse first = service.getWorkbench(date, null, "all", null, 1, null, true);
        HomeWorkbenchResponse second = service.getWorkbench(date, null, "all", null, 1,
                first.getPage().getNextCursor(), false);

        assertEquals("cleaning", first.getItems().get(0).getType());
        assertEquals("review", second.getItems().get(0).getType());
    }

    @Test
    void getWorkbench_reviewUsesSubmittedCountAndShowsRecentApprovedAsCompletedReadOnly() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        server.demo.dto.registration.AdminRegistrationListItemDTO submitted =
                new server.demo.dto.registration.AdminRegistrationListItemDTO();
        submitted.setFormId(71L);
        submitted.setGuestName("待审核住客");
        submitted.setStatus(RegistrationFormStatus.SUBMITTED);
        submitted.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 12, 0));

        server.demo.dto.registration.AdminRegistrationListItemDTO approved =
                new server.demo.dto.registration.AdminRegistrationListItemDTO();
        approved.setFormId(72L);
        approved.setGuestName("已完成住客");
        approved.setStatus(RegistrationFormStatus.APPROVED);
        approved.setApprovedAt(LocalDateTime.of(2026, 6, 11, 13, 0));

        when(registrationAdminService.listHomeSlice(any(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(submitted, approved));
        when(registrationAdminService.countHome(eq(RegistrationFormStatus.SUBMITTED), isNull())).thenReturn(1L);
        when(registrationAdminService.countHome(eq(RegistrationFormStatus.APPROVED), any())).thenReturn(1L);

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 50, "review");

        assertEquals(2, response.getItems().size());
        HomeWorkbenchItemDTO awaitingItem = response.getItems().stream()
                .filter(item -> "71".equals(item.getSourceId()))
                .findFirst().orElseThrow();
        HomeWorkbenchItemDTO completedItem = response.getItems().stream()
                .filter(item -> "72".equals(item.getSourceId()))
                .findFirst().orElseThrow();
        assertEquals("awaiting_review", awaitingItem.getStatusGroup());
        assertEquals("completed", completedItem.getStatusGroup());
        assertEquals(List.of("view"), awaitingItem.getActions().stream().map(action -> action.getCode()).toList());
        assertEquals(List.of("view"), completedItem.getActions().stream().map(action -> action.getCode()).toList());
        assertEquals(2L, response.getTypeSummaries().get(1).getCount());
        assertEquals(response.getPage().getTotalElements(), response.getTypeSummaries().get(1).getCount());
    }

    @Test
    void getWorkbench_typeCountsIncludeCompletedAndMatchAllTotal() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        InternalTaskDTO completedOne = internalTask(91L, InternalTaskStatus.COMPLETED, USER_ID, "员工A", false);
        InternalTaskDTO completedTwo = internalTask(92L, InternalTaskStatus.COMPLETED, USER_ID, "员工A", false);
        when(cleaningTaskService.getHomeTaskStatusCounts(USER_ID, businessDate))
                .thenReturn(java.util.Map.of("pending", 90L));
        when(registrationAdminService.countHome(eq(RegistrationFormStatus.SUBMITTED), isNull())).thenReturn(0L);
        when(registrationAdminService.countHome(eq(RegistrationFormStatus.APPROVED), any())).thenReturn(1L);
        when(internalTaskService.listHomeSlice(isNull(), any(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(completedOne, completedTwo));
        when(internalTaskService.countHome(eq(InternalTaskStatus.UNASSIGNED), any())).thenReturn(0L);
        when(internalTaskService.countHome(eq(InternalTaskStatus.ASSIGNED), any())).thenReturn(0L);
        when(internalTaskService.countHome(eq(InternalTaskStatus.COMPLETED), any())).thenReturn(2L);

        HomeWorkbenchResponse all = service.getWorkbench(businessDate, 50, "all");
        HomeWorkbenchResponse cleaning = service.getWorkbench(businessDate, 50, "cleaning");
        HomeWorkbenchResponse review = service.getWorkbench(businessDate, 50, "review");
        HomeWorkbenchResponse order = service.getWorkbench(businessDate, 50, "order");
        HomeWorkbenchResponse message = service.getWorkbench(businessDate, 50, "message");
        HomeWorkbenchResponse other = service.getWorkbench(businessDate, 50, "other");

        assertEquals(93L, all.getTypeSummaries().stream().mapToLong(summary -> summary.getCount()).sum());
        assertEquals(93L, all.getPage().getTotalElements());
        assertEquals(cleaning.getPage().getTotalElements(), cleaning.getTypeSummaries().get(0).getCount());
        assertEquals(1L, review.getTypeSummaries().get(1).getCount());
        assertEquals(review.getPage().getTotalElements(), review.getTypeSummaries().get(1).getCount());
        assertEquals(order.getPage().getTotalElements(), order.getTypeSummaries().get(2).getCount());
        assertEquals(message.getPage().getTotalElements(), message.getTypeSummaries().get(3).getCount());
        assertEquals(2L, other.getTypeSummaries().get(4).getCount());
        assertEquals(other.getPage().getTotalElements(), other.getTypeSummaries().get(4).getCount());
    }

    @Test
    void getWorkbench_reviewUsesStableCursorAndRealTotalBeyondFifty() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        List<server.demo.dto.registration.AdminRegistrationListItemDTO> forms = new ArrayList<>();
        for (long id = 1; id <= 51; id++) {
            server.demo.dto.registration.AdminRegistrationListItemDTO form =
                    new server.demo.dto.registration.AdminRegistrationListItemDTO();
            form.setFormId(id);
            form.setGuestName("住客 " + id);
            form.setStatus(RegistrationFormStatus.SUBMITTED);
            form.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 8, 0).plusMinutes(id));
            forms.add(form);
        }
        when(registrationAdminService.listHomeSlice(any(), eq(RegistrationFormStatus.SUBMITTED),
                any(), any(), any(), anyInt())).thenReturn(forms);
        when(registrationAdminService.countHome(eq(RegistrationFormStatus.SUBMITTED), isNull())).thenReturn(51L);

        HomeWorkbenchResponse first = service.getWorkbench(
                businessDate, null, "review", "awaiting_review", 50, null, true);
        HomeWorkbenchResponse second = service.getWorkbench(
                businessDate, null, "review", "awaiting_review", 50,
                first.getPage().getNextCursor(), false);

        assertEquals(51L, first.getPage().getTotalElements());
        assertEquals(50, first.getItems().size());
        assertTrue(first.getPage().isHasMore());
        assertEquals(1, second.getItems().size());
        assertFalse(second.getPage().isHasMore());
        assertEquals("51", second.getItems().get(0).getSourceId());
    }

    @Test
    void getWorkbench_rejectsCursorWhenFilterContextChanges() {
        HomeWorkbenchResponse first = service.getWorkbench(
                LocalDate.of(2026, 6, 12), null, "review", null, 1, null, true);
        if (first.getPage().getNextCursor() == null) {
            server.demo.dto.registration.AdminRegistrationListItemDTO firstForm =
                    new server.demo.dto.registration.AdminRegistrationListItemDTO();
            firstForm.setFormId(1L);
            firstForm.setStatus(RegistrationFormStatus.SUBMITTED);
            firstForm.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 10, 0));
            server.demo.dto.registration.AdminRegistrationListItemDTO secondForm =
                    new server.demo.dto.registration.AdminRegistrationListItemDTO();
            secondForm.setFormId(2L);
            secondForm.setStatus(RegistrationFormStatus.SUBMITTED);
            secondForm.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 11, 0));
            when(registrationAdminService.listHomeSlice(any(), isNull(),
                    isNull(), isNull(), isNull(), anyInt())).thenReturn(List.of(firstForm, secondForm));
            first = service.getWorkbench(
                    LocalDate.of(2026, 6, 12), null, "review", null, 1, null, true);
        }

        String cursor = first.getPage().getNextCursor();
        assertThrows(IllegalArgumentException.class, () -> service.getWorkbench(
                LocalDate.of(2026, 6, 12), null, "review", "completed", 1, cursor, false));
    }

    @Test
    void getWorkbench_rejectsCursorWhenPageSizeChanges() {
        server.demo.dto.registration.AdminRegistrationListItemDTO firstForm =
                new server.demo.dto.registration.AdminRegistrationListItemDTO();
        firstForm.setFormId(1L); firstForm.setStatus(RegistrationFormStatus.SUBMITTED);
        firstForm.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 10, 0));
        server.demo.dto.registration.AdminRegistrationListItemDTO secondForm =
                new server.demo.dto.registration.AdminRegistrationListItemDTO();
        secondForm.setFormId(2L); secondForm.setStatus(RegistrationFormStatus.SUBMITTED);
        secondForm.setSubmittedAt(LocalDateTime.of(2026, 6, 11, 11, 0));
        when(registrationAdminService.listHomeSlice(any(), isNull(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(firstForm, secondForm));
        HomeWorkbenchResponse first = service.getWorkbench(
                LocalDate.of(2026, 6, 12), null, "review", null, 1, null, true);

        assertThrows(IllegalArgumentException.class, () -> service.getWorkbench(
                LocalDate.of(2026, 6, 12), null, "review", null, 2,
                first.getPage().getNextCursor(), false));
    }

    @Test
    void getWorkbench_rejectsInactiveStoreMembershipBeforeLoadingAnyCard() {
        when(storeUserRepository.existsByStoreIdAndUserIdAndIsActiveTrue(STORE_ID, USER_ID)).thenReturn(false);

        assertThrows(StoreAccessDeniedException.class, () -> service.getWorkbench(null, 50));

        Mockito.verifyNoInteractions(cleaningTaskService, registrationAdminService, suMessagingService, internalTaskService);
    }

    @Test
    void getWorkbench_managerOtherUsesStoreManagedUnassignedAndAssignedTasks() {
        InternalTaskDTO unassigned = internalTask(81L, InternalTaskStatus.UNASSIGNED, null, null, false);
        InternalTaskDTO assigned = internalTask(82L, InternalTaskStatus.ASSIGNED, USER_ID, "员工A", true);
        when(internalTaskService.listHomeSlice(isNull(), any(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(unassigned, assigned));
        when(internalTaskService.countHome(eq(InternalTaskStatus.UNASSIGNED), any())).thenReturn(1L);
        when(internalTaskService.countHome(eq(InternalTaskStatus.ASSIGNED), any())).thenReturn(1L);

        HomeWorkbenchResponse response = service.getWorkbench(null, 50, "other");

        assertEquals(2, response.getItems().size());
        assertEquals(List.of("UNASSIGNED", "ASSIGNED"), response.getItems().stream()
                .map(HomeWorkbenchItemDTO::getSourceStatus).toList());
        assertEquals(2L, response.getTypeSummaries().get(4).getCount());
        assertTrue(response.getTypeSummaries().get(4).isConnected());
    }

    @Test
    void getWorkbench_memberOtherUsesOnlyMineWithoutManagedStoreAccess() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "MEMBER"));
        InternalTaskDTO assigned = internalTask(83L, InternalTaskStatus.ASSIGNED, USER_ID, "本人", true);
        when(internalTaskService.listHomeSlice(isNull(), any(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(assigned));
        when(internalTaskService.countHome(eq(InternalTaskStatus.ASSIGNED), any())).thenReturn(1L);

        HomeWorkbenchResponse response = service.getWorkbench(null, 50, "other");

        assertEquals(1, response.getItems().size());
        assertEquals("83", response.getItems().get(0).getSourceId());
        assertEquals(1L, response.getTypeSummaries().get(4).getCount());
        Mockito.verify(internalTaskService, never()).getManaged(any(), anyInt(), anyInt());
    }

    private static InternalTaskDTO internalTask(
            Long id,
            InternalTaskStatus status,
            Long assigneeUserId,
            String assigneeName,
            boolean canComplete
    ) {
        InternalTask task = new InternalTask();
        task.setId(id);
        task.setStoreId(STORE_ID);
        task.setTitle("任务 " + id);
        task.setDescription("任务说明");
        task.setStatus(status);
        task.setAssigneeUserId(assigneeUserId);
        task.setAssigneeNameSnapshot(assigneeName);
        task.setCreatedByUserId(1L);
        task.setCreatedByNameSnapshot("管理员");
        task.setCreatedAt(LocalDateTime.of(2026, 6, 11, 10, 0));
        task.setUpdatedAt(LocalDateTime.of(2026, 6, 11, 11, 0));
        if (status == InternalTaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.of(2026, 6, 11, 12, 0));
            task.setCompletedByUserId(assigneeUserId);
            task.setCompletedByNameSnapshot(assigneeName);
        }
        return InternalTaskDTO.from(task, canComplete, true);
    }

    private static Reservation buildReservation(Long id, String orderNumber, LocalDate checkInDate) {
        Channel channel = new Channel();
        channel.setId(2L);
        channel.setName("Booking.com");

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(STORE_ID);
        reservation.setOrderNumber(orderNumber);
        reservation.setGuestName("Guest " + id);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkInDate.plusDays(1));
        reservation.setChannel(channel);
        return reservation;
    }

    private static SuMessagingThreadDTO buildMessageThread(Long id, String guestName, String bookingId) {
        SuMessagingThreadDTO thread = new SuMessagingThreadDTO();
        thread.setId(id);
        thread.setGuestName(guestName);
        thread.setChannelName("Booking.com");
        thread.setBookingId(bookingId);
        thread.setLastMessage("Arrival time?");
        thread.setLastActivity(OffsetDateTime.parse("2026-06-12T01:00:00Z"));
        thread.setUnreadCount(3L);
        return thread;
    }
}
