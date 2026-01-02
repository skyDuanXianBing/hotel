package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SuContentPayloadBuilderTest {

    @Test
    void buildRoomCreatePayload_containsRequiredFields() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Deluxe King Room");
        roomType.setTotalRooms(2);
        roomType.setDefaultPrice(new BigDecimal("0"));

        Map<String, Object> payload = SuContentPayloadBuilder.buildRoomCreatePayload("STORE5", List.of(roomType));
        Map<?, ?> sellableProducts = (Map<?, ?>) payload.get("SellableProducts");
        assertEquals("STORE5", sellableProducts.get("hotelid"));

        List<?> sellableProductList = (List<?>) sellableProducts.get("SellableProduct");
        assertEquals(1, sellableProductList.size());

        Map<?, ?> sellableProduct = (Map<?, ?>) sellableProductList.get(0);
        assertEquals("Initial", sellableProduct.get("InvStatusType"));

        Map<?, ?> guestRoom = (Map<?, ?>) sellableProduct.get("GuestRoom");
        assertNotNull(guestRoom);

        Map<?, ?> occupancy = (Map<?, ?>) guestRoom.get("Occupancy");
        assertEquals("2", occupancy.get("MaxOccupancy"));
        assertEquals("0", occupancy.get("MaxChildOccupancy"));

        Map<?, ?> room = (Map<?, ?>) guestRoom.get("Room");
        assertEquals("1", room.get("roomid"));
        assertEquals("2", room.get("Quantity"));
        assertNotNull(room.get("RoomRate"));
        assertTrue(new BigDecimal(room.get("RoomRate").toString()).compareTo(BigDecimal.ZERO) > 0);

        Map<?, ?> description = (Map<?, ?>) guestRoom.get("Description");
        assertEquals("Deluxe King Room", description.get("Text"));
    }

    @Test
    void buildRoomCreatePayloadForRooms_usesRoomTypeIdAndRoomNumberAsRoomIdAndQuantityOne() {
        RoomType roomType = new RoomType();
        roomType.setId(99L);
        roomType.setName("Economy Double Room");
        roomType.setTotalRooms(10);
        roomType.setDefaultPrice(new BigDecimal("100.00"));

        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("Tanpopo Inn103");
        room.setRoomType(roomType);

        Map<String, Object> payload = SuContentPayloadBuilder.buildRoomCreatePayloadForRooms("STORE5", List.of(room));
        Map<?, ?> sellableProducts = (Map<?, ?>) payload.get("SellableProducts");
        assertEquals("STORE5", sellableProducts.get("hotelid"));

        List<?> sellableProductList = (List<?>) sellableProducts.get("SellableProduct");
        assertEquals(1, sellableProductList.size());

        Map<?, ?> sellableProduct = (Map<?, ?>) sellableProductList.get(0);
        Map<?, ?> guestRoom = (Map<?, ?>) sellableProduct.get("GuestRoom");
        Map<?, ?> roomNode = (Map<?, ?>) guestRoom.get("Room");

        assertEquals("99-Tanpopo Inn103", roomNode.get("roomid"));
        assertEquals("1", roomNode.get("Quantity"));

        Map<?, ?> description = (Map<?, ?>) guestRoom.get("Description");
        assertEquals("Economy Double Room", description.get("Text"));
    }

    @Test
    void buildRoomModifyPayload_usesOverlayModifyAndRoomId() {
        RoomType roomType = new RoomType();
        roomType.setId(12L);
        roomType.setName("Studio");
        roomType.setTotalRooms(1);
        roomType.setDefaultPrice(new BigDecimal("100.50"));

        Map<String, Object> payload = SuContentPayloadBuilder.buildRoomModifyPayload("STORE5", List.of(roomType));
        Map<?, ?> sellableProducts = (Map<?, ?>) payload.get("SellableProducts");
        List<?> sellableProductList = (List<?>) sellableProducts.get("SellableProduct");
        Map<?, ?> sellableProduct = (Map<?, ?>) sellableProductList.get(0);

        assertEquals("Overlay", sellableProduct.get("InvNotifType"));
        assertEquals("Modify", sellableProduct.get("InvStatusType"));
        assertEquals("12", sellableProduct.get("roomid"));
    }

    @Test
    void buildRoomModifyPayloadForRooms_usesOverlayModifyAndRoomId() {
        RoomType roomType = new RoomType();
        roomType.setId(99L);
        roomType.setName("Studio");
        roomType.setTotalRooms(2);
        roomType.setDefaultPrice(new BigDecimal("100.50"));

        Room room = new Room();
        room.setId(888L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);

        Map<String, Object> payload = SuContentPayloadBuilder.buildRoomModifyPayloadForRooms("STORE5", List.of(room));
        Map<?, ?> sellableProducts = (Map<?, ?>) payload.get("SellableProducts");
        List<?> sellableProductList = (List<?>) sellableProducts.get("SellableProduct");
        Map<?, ?> sellableProduct = (Map<?, ?>) sellableProductList.get(0);

        assertEquals("Overlay", sellableProduct.get("InvNotifType"));
        assertEquals("Modify", sellableProduct.get("InvStatusType"));
        assertEquals("99-101", sellableProduct.get("roomid"));

        Map<?, ?> guestRoom = (Map<?, ?>) sellableProduct.get("GuestRoom");
        Map<?, ?> description = (Map<?, ?>) guestRoom.get("Description");
        assertEquals("Studio", description.get("Text"));
    }

    @Test
    void buildRatePlanPayloads_haveIdsAndNames() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(5L);
        pricePlan.setName("BAR");

        Map<String, Object> createPayload = SuContentPayloadBuilder.buildRatePlanCreatePayload("STORE5", List.of(pricePlan));
        Map<?, ?> ratePlans = (Map<?, ?>) createPayload.get("RatePlans");
        assertEquals("STORE5", ratePlans.get("hotelid"));

        List<?> ratePlanList = (List<?>) ratePlans.get("RatePlan");
        Map<?, ?> ratePlan = (Map<?, ?>) ratePlanList.get(0);
        assertEquals("New", ratePlan.get("RatePlanNotifType"));
        assertEquals("5", ratePlan.get("rateplanid"));

        Map<?, ?> description = (Map<?, ?>) ratePlan.get("Description");
        assertEquals("BAR", description.get("Name"));
    }
}
