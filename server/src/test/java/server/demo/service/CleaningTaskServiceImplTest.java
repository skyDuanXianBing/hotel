package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CleaningTaskDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleaningTask;
import server.demo.entity.Room;
import server.demo.entity.User;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.UserRepository;
import server.demo.service.impl.CleaningTaskServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CleaningTaskServiceImplTest {

    private static final Long USER_ID = 88L;
    private static final Long STORE_ID = 26L;

    private CleaningTaskServiceImpl service;
    private CleaningTaskRepository cleaningTaskRepository;
    private UserRepository userRepository;
    private CleanerIdentityService cleanerIdentityService;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "member"));

        service = new CleaningTaskServiceImpl();
        cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        CleaningTaskAutoService cleaningTaskAutoService = Mockito.mock(CleaningTaskAutoService.class);
        cleanerIdentityService = Mockito.mock(CleanerIdentityService.class);

        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskAutoService", cleaningTaskAutoService);
        ReflectionTestUtils.setField(service, "cleanerIdentityService", cleanerIdentityService);
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
}
