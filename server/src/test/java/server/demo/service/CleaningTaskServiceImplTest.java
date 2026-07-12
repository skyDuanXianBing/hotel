package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CleaningTaskDTO;
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
import server.demo.service.impl.CleaningTaskServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CleaningTaskServiceImplTest {

    private static final Long USER_ID = 88L;
    private static final Long STORE_ID = 26L;

    private CleaningTaskServiceImpl service;
    private CleaningTaskRepository cleaningTaskRepository;
    private UserRepository userRepository;
    private StoreRepository storeRepository;
    private CleaningTaskAutoService cleaningTaskAutoService;
    private CleanerIdentityService cleanerIdentityService;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "member"));

        service = new CleaningTaskServiceImpl();
        cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        cleaningTaskAutoService = Mockito.mock(CleaningTaskAutoService.class);
        cleanerIdentityService = Mockito.mock(CleanerIdentityService.class);

        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskAutoService", cleaningTaskAutoService);
        ReflectionTestUtils.setField(service, "cleanerIdentityService", cleanerIdentityService);
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void completeTask_shouldUseCurrentUserAsApproverForCleaner() {
        Cleaner cleaner = new Cleaner();
        cleaner.setId(3L);

        CleaningTask task = new CleaningTask();
        task.setId(11L);
        task.setStatus("in_progress");
        task.setCleaner(cleaner);
        task.setRoom(new Room());

        User approver = new User();
        approver.setId(USER_ID);

        Mockito.when(cleanerIdentityService.findCleanerByUserIdAndStoreId(USER_ID, STORE_ID))
                .thenReturn(Optional.of(cleaner));
        Mockito.when(cleaningTaskRepository.findById(11L)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(approver));
        Mockito.when(cleaningTaskRepository.save(Mockito.any(CleaningTask.class))).thenAnswer(inv -> inv.getArgument(0));

        CleaningTaskDTO dto = service.completeTask(USER_ID, 11L, 999L);

        assertEquals(USER_ID, dto.getApproverId());
        Mockito.verify(userRepository).findById(USER_ID);
    }

    @Test
    void getTaskById_shouldRejectTaskFromAnotherCleaner() {
        Cleaner currentCleaner = new Cleaner();
        currentCleaner.setId(3L);

        Cleaner otherCleaner = new Cleaner();
        otherCleaner.setId(4L);

        CleaningTask task = new CleaningTask();
        task.setId(12L);
        task.setCleaner(otherCleaner);

        Mockito.when(cleanerIdentityService.findCleanerByUserIdAndStoreId(USER_ID, STORE_ID))
                .thenReturn(Optional.of(currentCleaner));
        Mockito.when(cleaningTaskRepository.findById(12L)).thenReturn(Optional.of(task));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getTaskById(USER_ID, 12L));
        assertEquals("只能查看或操作分配给自己的任务", exception.getMessage());
    }

    @Test
    void getTasksWithFilters_shouldMarkExpiredUsingStoreBusinessToday() {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setTimezone("Asia/Tokyo");

        Mockito.when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        Mockito.when(cleanerIdentityService.findCleanerByUserIdAndStoreId(USER_ID, STORE_ID))
                .thenReturn(Optional.empty());
        Mockito.when(cleaningTaskRepository.findWithFiltersByStore(
                Mockito.eq(STORE_ID),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.any(Pageable.class)
        )).thenReturn(Page.empty());

        service.getTasksWithFilters(USER_ID, null, null, null, null, null, null, null, null, Pageable.unpaged());

        Mockito.verify(cleaningTaskAutoService).markExpiredTasks(STORE_ID, LocalDate.of(2026, 4, 8));
    }

    @Test
    void getHomeTaskSlice_shouldPushCleanerScopeAndSizePlusOneWithoutStateWrite() {
        Cleaner cleaner = new Cleaner(); cleaner.setId(3L);
        Mockito.when(cleanerIdentityService.findCleanerByUserIdAndStoreId(USER_ID, STORE_ID))
                .thenReturn(Optional.of(cleaner));
        Mockito.when(cleaningTaskRepository.findHomeSlice(
                Mockito.eq(STORE_ID), Mockito.any(), Mockito.eq(3L), Mockito.eq("pending"),
                Mockito.any(), Mockito.eq(false), Mockito.anyInt(), Mockito.any(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(List.of());

        service.getHomeTaskSlice(USER_ID, LocalDate.of(2026, 4, 8), "pending",
                null, null, null, 51);

        Mockito.verify(cleaningTaskRepository).findHomeSlice(
                Mockito.eq(STORE_ID), Mockito.eq(LocalDate.of(2026, 4, 8)), Mockito.eq(3L),
                Mockito.eq("pending"), Mockito.eq(LocalDateTime.of(2026, 4, 8, 0, 0)),
                Mockito.eq(false), Mockito.eq(0), Mockito.any(), Mockito.eq(0L),
                Mockito.argThat(pageable -> pageable.getPageSize() == 51));
        Mockito.verifyNoInteractions(cleaningTaskAutoService);
    }

    @Test
    void getHomeTaskStatusCounts_ownerOrAdminScopeIsWholeStore() {
        Mockito.when(cleanerIdentityService.findCleanerByUserIdAndStoreId(USER_ID, STORE_ID))
                .thenReturn(Optional.empty());
        Mockito.when(cleaningTaskRepository.countHomeByStatus(
                STORE_ID, LocalDate.of(2026, 4, 8), null)).thenReturn(
                        java.util.Collections.singletonList(new Object[]{"pending", 51L}));

        assertEquals(51L, service.getHomeTaskStatusCounts(USER_ID, LocalDate.of(2026, 4, 8)).get("pending"));
        Mockito.verify(cleaningTaskRepository).countHomeByStatus(
                STORE_ID, LocalDate.of(2026, 4, 8), null);
    }
}
