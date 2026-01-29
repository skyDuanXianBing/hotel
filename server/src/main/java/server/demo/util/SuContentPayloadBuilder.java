package server.demo.util;

import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 构造 Su Content API（房型/费率计划）请求体。
 * <p>
 * 目标：让 Su Widget 的 “PMS 房型 / 价格计划” 下拉能拿到数据（避免 No Record Found）。
 */
public final class SuContentPayloadBuilder {

    private SuContentPayloadBuilder() {}

    /**
     * 用于 Su Widget 映射下拉：将 PMS 的“房间列表（按房间号）”同步到 Su。
     * <p>
     * 约束：Su 的 roomid 最长 20 字符；本项目使用 rooms.room_number（<=20）作为 roomid，避免与房型ID冲突。
     */
    public static Map<String, Object> buildRoomCreatePayloadForRooms(String hotelId, List<Room> rooms) {
        Map<String, Object> sellableProducts = new HashMap<>();
        sellableProducts.put("hotelid", hotelId);

        List<Map<String, Object>> sellableProductList = new ArrayList<>();
        for (Room roomEntity : rooms) {
            String roomId = SuRoomIdUtil.buildRoomId(roomEntity);
            RoomType roomType = roomEntity.getRoomType();

            Map<String, Object> room = new HashMap<>();
            room.put("roomid", roomId);
            room.put("RoomRate", formatPositiveRate(roomType != null ? roomType.getDefaultPrice() : null));
            room.put("Quantity", "1");
            room.put("RoomType", inferSuRoomType(roomType != null ? roomType.getName() : null));

            Map<String, Object> occupancy = new HashMap<>();
            occupancy.put("MaxOccupancy", String.valueOf(resolveMaxOccupancy(roomType)));
            occupancy.put("MaxChildOccupancy", "0");

            Map<String, Object> description = new HashMap<>();
            description.put("Text", SuRoomIdUtil.buildDisplayName(roomEntity));

            Map<String, Object> guestRoom = new HashMap<>();
            guestRoom.put("Occupancy", occupancy);
            guestRoom.put("Room", room);
            guestRoom.put("Description", description);

            Map<String, Object> sellableProduct = new HashMap<>();
            sellableProduct.put("InvStatusType", "Initial");
            sellableProduct.put("GuestRoom", guestRoom);

            sellableProductList.add(sellableProduct);
        }

        sellableProducts.put("SellableProduct", sellableProductList);

        Map<String, Object> root = new HashMap<>();
        root.put("SellableProducts", sellableProducts);
        return root;
    }

    public static Map<String, Object> buildRoomCreatePayload(String hotelId, List<RoomType> roomTypes) {
        Map<String, Object> sellableProducts = new HashMap<>();
        sellableProducts.put("hotelid", hotelId);

        List<Map<String, Object>> sellableProductList = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            String roomId = toSuId(roomType.getId());

            Map<String, Object> room = new HashMap<>();
            room.put("roomid", roomId);
            room.put("RoomRate", formatPositiveRate(roomType.getDefaultPrice()));
            room.put("Quantity", String.valueOf(roomType.getTotalRooms() != null ? roomType.getTotalRooms() : 1));
            room.put("RoomType", inferSuRoomType(roomType.getName()));

            Map<String, Object> occupancy = new HashMap<>();
            occupancy.put("MaxOccupancy", String.valueOf(resolveMaxOccupancy(roomType)));
            occupancy.put("MaxChildOccupancy", "0");

            Map<String, Object> description = new HashMap<>();
            description.put("Text", roomType.getName());

            Map<String, Object> guestRoom = new HashMap<>();
            guestRoom.put("Occupancy", occupancy);
            guestRoom.put("Room", room);
            guestRoom.put("Description", description);

            Map<String, Object> sellableProduct = new HashMap<>();
            sellableProduct.put("InvStatusType", "Initial");
            sellableProduct.put("GuestRoom", guestRoom);

            sellableProductList.add(sellableProduct);
        }

        sellableProducts.put("SellableProduct", sellableProductList);

        Map<String, Object> root = new HashMap<>();
        root.put("SellableProducts", sellableProducts);
        return root;
    }

    public static Map<String, Object> buildRoomModifyPayloadForRooms(String hotelId, List<Room> rooms) {
        Map<String, Object> sellableProducts = new HashMap<>();
        sellableProducts.put("hotelid", hotelId);

        List<Map<String, Object>> sellableProductList = new ArrayList<>();
        for (Room roomEntity : rooms) {
            String roomId = SuRoomIdUtil.buildRoomId(roomEntity);
            RoomType roomType = roomEntity.getRoomType();

            Map<String, Object> room = new HashMap<>();
            room.put("RoomRate", formatPositiveRate(roomType != null ? roomType.getDefaultPrice() : null));
            room.put("Quantity", "1");
            room.put("RoomType", inferSuRoomType(roomType != null ? roomType.getName() : null));

            Map<String, Object> description = new HashMap<>();
            description.put("Text", SuRoomIdUtil.buildDisplayName(roomEntity));

            Map<String, Object> guestRoom = new HashMap<>();
            Map<String, Object> occupancy = new HashMap<>();
            occupancy.put("MaxOccupancy", String.valueOf(resolveMaxOccupancy(roomType)));
            occupancy.put("MaxChildOccupancy", "0");
            guestRoom.put("Occupancy", occupancy);
            guestRoom.put("Room", room);
            guestRoom.put("Description", description);

            Map<String, Object> sellableProduct = new HashMap<>();
            sellableProduct.put("InvNotifType", "Overlay");
            sellableProduct.put("InvStatusType", "Modify");
            sellableProduct.put("roomid", roomId);
            sellableProduct.put("GuestRoom", guestRoom);

            sellableProductList.add(sellableProduct);
        }

        sellableProducts.put("SellableProduct", sellableProductList);

        Map<String, Object> root = new HashMap<>();
        root.put("SellableProducts", sellableProducts);
        return root;
    }

    public static Map<String, Object> buildRoomModifyPayload(String hotelId, List<RoomType> roomTypes) {
        Map<String, Object> sellableProducts = new HashMap<>();
        sellableProducts.put("hotelid", hotelId);

        List<Map<String, Object>> sellableProductList = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            String roomId = toSuId(roomType.getId());

            Map<String, Object> room = new HashMap<>();
            room.put("RoomRate", formatPositiveRate(roomType.getDefaultPrice()));
            room.put("Quantity", String.valueOf(roomType.getTotalRooms() != null ? roomType.getTotalRooms() : 1));
            room.put("RoomType", inferSuRoomType(roomType.getName()));

            Map<String, Object> description = new HashMap<>();
            description.put("Text", roomType.getName());

            Map<String, Object> guestRoom = new HashMap<>();
            Map<String, Object> occupancy = new HashMap<>();
            occupancy.put("MaxOccupancy", String.valueOf(resolveMaxOccupancy(roomType)));
            occupancy.put("MaxChildOccupancy", "0");
            guestRoom.put("Occupancy", occupancy);
            guestRoom.put("Room", room);
            guestRoom.put("Description", description);

            Map<String, Object> sellableProduct = new HashMap<>();
            sellableProduct.put("InvNotifType", "Overlay");
            sellableProduct.put("InvStatusType", "Modify");
            sellableProduct.put("roomid", roomId);
            sellableProduct.put("GuestRoom", guestRoom);

            sellableProductList.add(sellableProduct);
        }

        sellableProducts.put("SellableProduct", sellableProductList);

        Map<String, Object> root = new HashMap<>();
        root.put("SellableProducts", sellableProducts);
        return root;
    }

    public static Map<String, Object> buildRatePlanCreatePayload(String hotelId, List<PricePlan> pricePlans) {
        return buildRatePlanPayload(hotelId, pricePlans, "New");
    }

    public static Map<String, Object> buildRatePlanOverlayPayload(String hotelId, List<PricePlan> pricePlans) {
        return buildRatePlanPayload(hotelId, pricePlans, "Overlay");
    }

    private static Map<String, Object> buildRatePlanPayload(String hotelId, List<PricePlan> pricePlans, String ratePlanNotifType) {
        Map<String, Object> ratePlans = new HashMap<>();
        ratePlans.put("hotelid", hotelId);

        List<Map<String, Object>> ratePlanList = new ArrayList<>();
        for (PricePlan pricePlan : pricePlans) {
            String ratePlanId = toSuId(pricePlan.getId());

            Map<String, Object> description = new HashMap<>();
            description.put("Name", pricePlan.getName());
            if (pricePlan.getDescription() != null && !pricePlan.getDescription().isBlank()) {
                description.put("Text", pricePlan.getDescription());
            } else {
                description.put("Text", pricePlan.getName());
            }

            Map<String, Object> ratePlan = new HashMap<>();
            ratePlan.put("RatePlanNotifType", ratePlanNotifType);
            ratePlan.put("rateplanid", ratePlanId);
            // MealPlanID 可选：不传也会使用默认值；这里显式传默认值以提高兼容性（15 = Room Only）
            ratePlan.put("MealPlanID", "15");
            ratePlan.put("Description", description);

            ratePlanList.add(ratePlan);
        }

        ratePlans.put("RatePlan", ratePlanList);

        Map<String, Object> root = new HashMap<>();
        root.put("RatePlans", ratePlans);
        return root;
    }

    private static String toSuRoomId(String roomNumber) {
        if (roomNumber == null) {
            throw new IllegalArgumentException("Su roomid 不能为空");
        }
        String trimmed = roomNumber.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Su roomid 不能为空");
        }
        if (trimmed.length() > 20) {
            throw new IllegalArgumentException("Su roomid 长度不能超过20: " + trimmed);
        }
        return trimmed;
    }

    private static String toSuId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Su roomid/rateplanid 不能为空");
        }
        String value = id.toString();
        if (value.length() > 20) {
            throw new IllegalArgumentException("Su roomid/rateplanid 长度不能超过20: " + value);
        }
        return value;
    }

    private static int resolveMaxOccupancy(RoomType roomType) {
        // 用户确认：兜底希望为 1
        final int fallback = 1;
        final int maxLimit = 30;

        Integer maxGuests = roomType != null ? roomType.getMaxGuests() : null;
        if (maxGuests == null || maxGuests < 1) {
            return fallback;
        }
        return Math.min(maxGuests, maxLimit);
    }

    private static String formatPositiveRate(BigDecimal value) {
        if (value == null) {
            return "1";
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return "1";
        }
        return normalized.stripTrailingZeros().toPlainString();
    }

    private static String inferSuRoomType(String name) {
        if (name == null || name.isBlank()) {
            return "Apartment";
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("king") || normalized.contains("大床")) {
            return "King";
        }
        if (normalized.contains("queen")) {
            return "Queen";
        }
        if (normalized.contains("twin") || normalized.contains("双床")) {
            return "Twin";
        }
        if (normalized.contains("single") || normalized.contains("单人")) {
            return "Single";
        }
        if (normalized.contains("double") || normalized.contains("双人")) {
            return "Double";
        }
        if (normalized.contains("dorm")) {
            return "Dormitory";
        }
        if (normalized.contains("family")) {
            return "Family";
        }
        if (normalized.contains("studio")) {
            return "Studio";
        }
        return "Apartment";
    }
}
