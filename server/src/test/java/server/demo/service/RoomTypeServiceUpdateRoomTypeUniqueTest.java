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
import server.demo.repository.RoomTypeRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTypeServiceUpdateRoomTypeUniqueTest {

    private static final long STORE_ID = 13L;
    private static final long USER_ID = 1L;

    private RoomTypeService service;
    private RoomTypeRepository roomTypeRepository;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        service = new RoomTypeService();
        roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void updateRoomType_sameCodeDifferentCaseOrSpaces_shouldNotFailAndShouldNormalize() {
        long roomTypeId = 99L;

        RoomType existing = new RoomType();
        existing.setId(roomTypeId);
        existing.setStoreId(STORE_ID);
        existing.setName("Deluxe");
        existing.setCode("DLX");
        existing.setTotalRooms(2);
        existing.setMaxGuests(2);

        Mockito.when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(existing));
        Mockito.when(roomTypeRepository.save(Mockito.any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomType incoming = new RoomType();
        incoming.setName("  deluxe  ");
        incoming.setCode("  dlx ");
        incoming.setTotalRooms(2);
        incoming.setMaxGuests(2);

        RoomType saved = service.updateRoomType(roomTypeId, incoming);
        assertEquals("DLX", saved.getCode());
        assertEquals("deluxe", saved.getName());

        Mockito.verify(roomTypeRepository, Mockito.never())
                .existsByStoreIdAndCodeAndIdNot(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());

        ArgumentCaptor<RoomType> captor = ArgumentCaptor.forClass(RoomType.class);
        Mockito.verify(roomTypeRepository).save(captor.capture());
        assertEquals("DLX", captor.getValue().getCode());
        assertEquals("deluxe", captor.getValue().getName());
    }

    @Test
    void updateRoomType_codeConflicts_shouldThrow() {
        long roomTypeId = 100L;

        RoomType existing = new RoomType();
        existing.setId(roomTypeId);
        existing.setStoreId(STORE_ID);
        existing.setName("Deluxe");
        existing.setCode("DLX");
        existing.setTotalRooms(2);
        existing.setMaxGuests(2);

        Mockito.when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(existing));
        Mockito.when(roomTypeRepository.existsByStoreIdAndCodeAndIdNot(STORE_ID, "ABC", roomTypeId)).thenReturn(true);

        RoomType incoming = new RoomType();
        incoming.setName("Deluxe");
        incoming.setCode("abc");
        incoming.setTotalRooms(2);
        incoming.setMaxGuests(2);

        assertThrows(RuntimeException.class, () -> service.updateRoomType(roomTypeId, incoming));
    }
}

