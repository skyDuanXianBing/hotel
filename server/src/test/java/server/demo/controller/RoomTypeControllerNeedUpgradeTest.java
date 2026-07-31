package server.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.ApiResponse;
import server.demo.dto.CreateRoomTypeRequest;
import server.demo.entity.RoomType;
import server.demo.exception.NeedUpgradeException;
import server.demo.service.RoomTypeService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 房型接口不得吞掉 SaaS 容量超限的 {@link NeedUpgradeException}：
 * 必须原样抛出，交给 SaasEntitlementExceptionHandler 统一返回 HTTP 402。
 */
class RoomTypeControllerNeedUpgradeTest {

    private RoomTypeController controller;
    private RoomTypeService roomTypeService;

    @BeforeEach
    void setUp() {
        controller = new RoomTypeController();
        roomTypeService = Mockito.mock(RoomTypeService.class);
        ReflectionTestUtils.setField(controller, "roomTypeService", roomTypeService);
    }

    private static CreateRoomTypeRequest newRequest() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Test room type");
        request.setCode("TST");
        request.setTotalRooms(1);
        request.setMaxGuests(2);
        request.setRoomNumbers(List.of("101"));
        return request;
    }

    private static NeedUpgradeException capacityExceeded() {
        return new NeedUpgradeException("ROOM_COUNT", 10L, 10L, "房间数量已达套餐上限，请升级套餐");
    }

    @Test
    void createRoomType_capacityExceeded_rethrowsNeedUpgradeException() {
        NeedUpgradeException expected = capacityExceeded();
        Mockito.when(roomTypeService.createRoomTypeWithRoomInputs(Mockito.any(RoomType.class), Mockito.anyList()))
                .thenThrow(expected);

        NeedUpgradeException thrown = assertThrows(NeedUpgradeException.class,
                () -> controller.createRoomType(newRequest()));

        assertSame(expected, thrown);
        assertEquals("ROOM_COUNT", thrown.getFeatureCode());
        assertEquals(10L, thrown.getLimit());
        assertEquals(10L, thrown.getUsed());
    }

    @Test
    void updateRoomType_capacityExceeded_rethrowsNeedUpgradeException() {
        NeedUpgradeException expected = capacityExceeded();
        Mockito.when(roomTypeService.updateRoomTypeWithRoomInputs(Mockito.eq(7L), Mockito.any(RoomType.class), Mockito.anyList()))
                .thenThrow(expected);

        NeedUpgradeException thrown = assertThrows(NeedUpgradeException.class,
                () -> controller.updateRoomType(7L, newRequest()));

        assertSame(expected, thrown);
    }

    @Test
    void createRoomType_otherException_stillReturnsApiResponseError() {
        Mockito.when(roomTypeService.createRoomTypeWithRoomInputs(Mockito.any(RoomType.class), Mockito.anyList()))
                .thenThrow(new RuntimeException("Room number 101 already exists"));

        ApiResponse<RoomType> response = controller.createRoomType(newRequest());

        assertFalse(response.isSuccess());
        assertEquals("房型创建失败: Room number 101 already exists", response.getMessage());
    }

    @Test
    void updateRoomType_otherException_stillReturnsApiResponseError() {
        Mockito.when(roomTypeService.updateRoomTypeWithRoomInputs(Mockito.eq(7L), Mockito.any(RoomType.class), Mockito.anyList()))
                .thenThrow(new RuntimeException("房型不存在"));

        ApiResponse<RoomType> response = controller.updateRoomType(7L, newRequest());

        assertFalse(response.isSuccess());
        assertEquals("房型更新失败: 房型不存在", response.getMessage());
    }
}
