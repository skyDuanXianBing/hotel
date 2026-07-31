package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.constants.SaasFeatureCodes;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.exception.NeedUpgradeException;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;
import server.demo.service.RoomTypeService.RoomInput;
import server.demo.service.saas.EntitlementService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 房型房间数变化的 SaaS 容量埋点（QA D6 缺口）：
 * 创建路径 delta=新增房间数；更新路径 delta=净增（新增−删除，rename 不计）；
 * delta&lt;=0 不触发校验；校验失败 NeedUpgradeException 原样上抛且不落任何房间。
 * mock 方式对齐 RoomTypeServiceUniqueCodeTest / RoomTypeServiceDeleteRoomTypeBlockedTest。
 */
class RoomTypeServiceCapacityCheckTest {

    private static final long STORE_ID = 13L;
    private static final long USER_ID = 1L;
    private static final long ROOM_TYPE_ID = 40L;

    private RoomTypeService service;
    private RoomTypeRepository roomTypeRepository;
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private UserRepository userRepository;
    private CleaningTaskRepository cleaningTaskRepository;
    private RoomBlockoutRepository roomBlockoutRepository;
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        service = new RoomTypeService();
        roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        roomRepository = Mockito.mock(RoomRepository.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);
        entitlementService = Mockito.mock(EntitlementService.class);

        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);
        ReflectionTestUtils.setField(service, "entitlementService", entitlementService);

        User user = new User();
        user.setId(USER_ID);
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // 创建路径
    // ------------------------------------------------------------------

    private RoomType newRoomTypePayload() {
        RoomType roomType = new RoomType();
        roomType.setName("Deluxe");
        roomType.setCode("DLX");
        roomType.setTotalRooms(3);
        roomType.setMaxGuests(2);
        return roomType;
    }

    @Test
    void createRoomTypeWithRoomInputs_capacityCheckDeltaEqualsNewRoomCount() {
        Mockito.when(roomRepository.countByStoreId(STORE_ID)).thenReturn(5L);

        service.createRoomTypeWithRoomInputs(newRoomTypePayload(),
                List.of(new RoomInput("101", null), new RoomInput("102", null), new RoomInput("103", null)));

        // delta = 新增房间数 3；current = 当前门店房间总数 5
        Mockito.verify(entitlementService).checkCapacity(
                STORE_ID, SaasFeatureCodes.ROOM_COUNT, 5L, 3L);
    }

    @Test
    void createRoomTypeWithRoomInputs_emptyRoomList_skipsCapacityCheck() {
        service.createRoomTypeWithRoomInputs(newRoomTypePayload(), List.of());

        Mockito.verifyNoInteractions(entitlementService);
        Mockito.verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void createRoomTypeWithRoomInputs_capacityExceeded_propagatesAndCreatesNoRoom() {
        Mockito.when(roomRepository.countByStoreId(STORE_ID)).thenReturn(9L);
        NeedUpgradeException expected = new NeedUpgradeException(
                SaasFeatureCodes.ROOM_COUNT, 10L, 9L, "已达到套餐的房间数量上限（10），请升级套餐后再新增");
        Mockito.doThrow(expected).when(entitlementService)
                .checkCapacity(STORE_ID, SaasFeatureCodes.ROOM_COUNT, 9L, 2L);

        NeedUpgradeException thrown = assertThrows(NeedUpgradeException.class,
                () -> service.createRoomTypeWithRoomInputs(newRoomTypePayload(),
                        List.of(new RoomInput("101", null), new RoomInput("102", null))));

        assertSame(expected, thrown);
        assertEquals(SaasFeatureCodes.ROOM_COUNT, thrown.getFeatureCode());
        assertEquals(10L, thrown.getLimit());
        assertEquals(9L, thrown.getUsed());
        // 容量校验先于房间创建：超限时不落任何房间（单事务回滚由 service 层 @Transactional 保证）
        Mockito.verify(roomRepository, never()).save(any(Room.class));
    }

    // ------------------------------------------------------------------
    // 更新路径
    // ------------------------------------------------------------------

    private Room existingRoom(Long id, String roomNumber) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber(roomNumber);
        room.setStoreId(STORE_ID);
        return room;
    }

    /** 与存量同 name/code 的更新载荷（跳过唯一性冲突分支），maxGuests 合法。 */
    private void stubExistingRoomType() {
        RoomType existing = new RoomType();
        existing.setId(ROOM_TYPE_ID);
        existing.setStoreId(STORE_ID);
        existing.setName("Deluxe");
        existing.setCode("DLX");
        existing.setTotalRooms(2);
        existing.setMaxGuests(2);
        Mockito.when(roomTypeRepository.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(existing));
    }

    private RoomType updatePayload() {
        RoomType incoming = new RoomType();
        incoming.setName("Deluxe");
        incoming.setCode("DLX");
        incoming.setTotalRooms(4);
        incoming.setMaxGuests(2);
        return incoming;
    }

    @Test
    void updateRoomTypeWithRoomInputs_capacityCheckDeltaIsNetIncrease() {
        stubExistingRoomType();
        Mockito.when(roomRepository.findByStoreIdAndRoomTypeId(STORE_ID, ROOM_TYPE_ID))
                .thenReturn(List.of(existingRoom(1L, "101"), existingRoom(2L, "102")));
        Mockito.when(roomRepository.countByStoreId(STORE_ID)).thenReturn(8L);

        service.updateRoomTypeWithRoomInputs(ROOM_TYPE_ID, updatePayload(),
                List.of(new RoomInput("101", null), new RoomInput("102", null),
                        new RoomInput("103", null), new RoomInput("104", null)));

        // 净增 = 新增 2（103/104） − 删除 0 → delta=2；current = 当前门店房间总数 8
        Mockito.verify(entitlementService).checkCapacity(
                STORE_ID, SaasFeatureCodes.ROOM_COUNT, 8L, 2L);
    }

    @Test
    void updateRoomTypeWithRoomInputs_pureRename_deltaZeroSkipsCapacityCheck() {
        stubExistingRoomType();
        Room room101 = existingRoom(1L, "101");
        Room room102 = existingRoom(2L, "102");
        Mockito.when(roomRepository.findByStoreIdAndRoomTypeId(STORE_ID, ROOM_TYPE_ID))
                .thenReturn(List.of(room101, room102));

        service.updateRoomTypeWithRoomInputs(ROOM_TYPE_ID, updatePayload(),
                List.of(new RoomInput("201", null), new RoomInput("202", null)));

        // 纯改名：rename 复用 room_id 不增减房间数，delta=0，不触发容量校验
        Mockito.verify(entitlementService, never())
                .checkCapacity(anyLong(), anyString(), anyLong(), anyLong());
        assertEquals("201", room101.getRoomNumber());
        assertEquals("202", room102.getRoomNumber());
        // rename 就地保存，无新增房间、无删除
        Mockito.verify(roomRepository, Mockito.times(2)).save(any(Room.class));
        Mockito.verify(roomRepository, never()).deleteAll(anyList());
    }

    @Test
    void updateRoomTypeWithRoomInputs_netDecrease_skipsCapacityCheck() {
        stubExistingRoomType();
        Mockito.when(roomRepository.findByStoreIdAndRoomTypeId(STORE_ID, ROOM_TYPE_ID))
                .thenReturn(List.of(
                        existingRoom(1L, "101"), existingRoom(2L, "102"), existingRoom(3L, "103")));

        service.updateRoomTypeWithRoomInputs(ROOM_TYPE_ID, updatePayload(),
                List.of(new RoomInput("101", null)));

        // 净减 2 间（delta=-2）：软限制只阻断新增，不触发容量校验
        Mockito.verify(entitlementService, never())
                .checkCapacity(anyLong(), anyString(), anyLong(), anyLong());
        // 被移除房间走预订解绑 + 清理任务/封房清理 + 删除链路
        Mockito.verify(reservationRepository)
                .clearRoomBindingByStoreIdAndRoomIds(eq(STORE_ID), anyList());
        Mockito.verify(roomRepository).deleteAll(anyList());
    }

    @Test
    void updateRoomTypeWithRoomInputs_capacityExceeded_propagatesAndCreatesNoRoom() {
        stubExistingRoomType();
        Mockito.when(roomRepository.findByStoreIdAndRoomTypeId(STORE_ID, ROOM_TYPE_ID))
                .thenReturn(List.of(existingRoom(1L, "101")));
        Mockito.when(roomRepository.countByStoreId(STORE_ID)).thenReturn(10L);
        NeedUpgradeException expected = new NeedUpgradeException(
                SaasFeatureCodes.ROOM_COUNT, 10L, 10L, "已达到套餐的房间数量上限（10），请升级套餐后再新增");
        Mockito.doThrow(expected).when(entitlementService)
                .checkCapacity(STORE_ID, SaasFeatureCodes.ROOM_COUNT, 10L, 2L);

        NeedUpgradeException thrown = assertThrows(NeedUpgradeException.class,
                () -> service.updateRoomTypeWithRoomInputs(ROOM_TYPE_ID, updatePayload(),
                        List.of(new RoomInput("101", null), new RoomInput("102", null),
                                new RoomInput("103", null))));

        assertSame(expected, thrown);
        // 校验失败：既不新增房间也不删除存量（整体回滚由 @Transactional 保证）
        Mockito.verify(roomRepository, never()).save(any(Room.class));
        Mockito.verify(roomRepository, never()).deleteAll(anyList());
    }
}
