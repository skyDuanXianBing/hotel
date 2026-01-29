package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomTypeServiceUniqueCodeTest {

    private static final long STORE_ID = 13L;
    private static final long USER_ID = 1L;

    private RoomTypeService service;
    private RoomTypeRepository roomTypeRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        service = new RoomTypeService();
        roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createRoomType_codeConflicts_appendsNumericSuffix() {
        User user = new User();
        user.setId(USER_ID);
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Mockito.when(roomTypeRepository.existsByStoreIdAndCode(STORE_ID, "TES")).thenReturn(true);
        Mockito.when(roomTypeRepository.existsByStoreIdAndCode(STORE_ID, "TES1")).thenReturn(false);
        Mockito.when(roomTypeRepository.save(Mockito.any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomType roomType = new RoomType();
        roomType.setName("Test room type3");
        roomType.setCode("tes");
        roomType.setTotalRooms(1);

        RoomType created = service.createRoomType(roomType);
        assertEquals("TES1", created.getCode());

        ArgumentCaptor<RoomType> captor = ArgumentCaptor.forClass(RoomType.class);
        Mockito.verify(roomTypeRepository).save(captor.capture());
        assertEquals("TES1", captor.getValue().getCode());
    }
}

