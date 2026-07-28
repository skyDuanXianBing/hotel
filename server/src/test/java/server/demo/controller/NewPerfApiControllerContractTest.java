package server.demo.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.OrderBoxDTO;
import server.demo.dto.RoomGroupWithMembersDTO;
import server.demo.entity.RoomGroup;
import server.demo.service.OrderBoxService;
import server.demo.service.PricePlanService;
import server.demo.service.RoomGroupService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewPerfApiControllerContractTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void roomGroupsWithMembers_shouldExposeEndpointAndReturnServiceData() throws Exception {
        RoomGroupService roomGroupService = mock(RoomGroupService.class);
        RoomGroupController controller = new RoomGroupController();
        ReflectionTestUtils.setField(controller, "roomGroupService", roomGroupService);

        RoomGroup group = new RoomGroup();
        group.setId(1L);
        group.setName("A");
        when(roomGroupService.getAllWithMembersForCurrentStore())
                .thenReturn(List.of(new RoomGroupWithMembersDTO(group, List.of())));

        ResponseEntity<ApiResponse<List<RoomGroupWithMembersDTO>>> response = controller.getAllWithMembers();

        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        verify(roomGroupService).getAllWithMembersForCurrentStore();

        Method method = RoomGroupController.class.getMethod("getAllWithMembers");
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/with-members", mapping.value()[0]);
    }

    @Test
    void orderBoxByReservation_shouldReturnItemOrNull() throws Exception {
        OrderBoxService orderBoxService = mock(OrderBoxService.class);
        OrderBoxController controller = new OrderBoxController();
        ReflectionTestUtils.setField(controller, "orderBoxService", orderBoxService);

        OrderBoxDTO dto = new OrderBoxDTO();
        dto.setId(5L);
        when(orderBoxService.getOrderBoxItemByReservationId(88L)).thenReturn(dto);
        when(orderBoxService.getOrderBoxItemByReservationId(99L)).thenReturn(null);

        ApiResponse<OrderBoxDTO> found = controller.getOrderBoxItemByReservation(88L);
        ApiResponse<OrderBoxDTO> missing = controller.getOrderBoxItemByReservation(99L);

        assertTrue(found.isSuccess());
        assertEquals(5L, found.getData().getId());
        assertTrue(missing.isSuccess());
        assertNull(missing.getData());

        Method method = OrderBoxController.class.getMethod("getOrderBoxItemByReservation", Long.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/by-reservation/{reservationId}", mapping.value()[0]);
    }

    @Test
    void pricePlanRoomTypeCounts_shouldExposeEndpointAndReturnMap() throws Exception {
        PricePlanService pricePlanService = mock(PricePlanService.class);
        PricePlanController controller = new PricePlanController();
        ReflectionTestUtils.setField(controller, "pricePlanService", pricePlanService);
        when(pricePlanService.countRoomTypesByPricePlanForCurrentStore())
                .thenReturn(Map.of(10L, 3L));

        ResponseEntity<ApiResponse<Map<Long, Long>>> response = controller.countRoomTypesByPricePlans();

        assertTrue(response.getBody().isSuccess());
        assertEquals(3L, response.getBody().getData().get(10L));
        verify(pricePlanService).countRoomTypesByPricePlanForCurrentStore();

        Method method = PricePlanController.class.getMethod("countRoomTypesByPricePlans");
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/room-type-counts", mapping.value()[0]);
    }
}
