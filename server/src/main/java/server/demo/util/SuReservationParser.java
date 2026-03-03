package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 将 Su 的 Reservation API 响应解析为 PMS 可用字段（尽量做兼容处理）。
 *
 * 参考文档：
 * - POST /SUAPI/jservice/Reservation
 * - POST /SUAPI/jservice/Reservation_notif
 */
public final class SuReservationParser {

    private SuReservationParser() {}

    public static List<JsonNode> extractReservationNodes(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        JsonNode reservations = root.get("reservations");
        if (reservations == null || !reservations.isArray()) {
            return List.of();
        }

        List<JsonNode> list = new ArrayList<>();
        for (JsonNode item : reservations) {
            if (item == null || item.isNull()) {
                continue;
            }
            JsonNode reservation = item.get("reservation");
            list.add(reservation != null && reservation.isObject() ? reservation : item);
        }
        return list;
    }

    public static List<JsonNode> extractRoomStays(JsonNode reservation) {
        if (reservation == null || reservation.isNull()) {
            return List.of();
        }
        JsonNode rooms = reservation.get("rooms");
        if (rooms != null && rooms.isArray() && rooms.size() > 0) {
            List<JsonNode> list = new ArrayList<>();
            rooms.forEach(list::add);
            return list;
        }
        JsonNode room = reservation.get("room");
        if (room != null && room.isArray() && room.size() > 0) {
            List<JsonNode> list = new ArrayList<>();
            room.forEach(list::add);
            return list;
        }
        return List.of();
    }

    public static String extractReservationNotifId(JsonNode reservation) {
        return text(reservation, "reservation_notif_id")
                .orElse(null);
    }

    public static String extractReservationId(JsonNode reservation) {
        return text(reservation, "id").orElse(null);
    }

    public static String extractChannelBookingId(JsonNode reservation) {
        return text(reservation, "channel_booking_id")
                .or(() -> text(reservation, "channel_bookingid"))
                .or(() -> text(reservation, "channel_booking_id".toUpperCase(Locale.ROOT)))
                .orElse(null);
    }

    /**
     * 用于消息系统的 thread id（常见于 Airbnb）。
     * 兼容字段：thread_id / threadid / thread_id(大写)。
     */
    public static String extractThreadId(JsonNode reservation) {
        return text(reservation, "thread_id")
                .or(() -> text(reservation, "threadid"))
                .or(() -> text(reservation, "thread_id".toUpperCase(Locale.ROOT)))
                .orElse(null);
    }

    /**
     * 用于消息系统的 guest id（常见于 Airbnb）。
     * 兼容字段：guest_id / guestid / guest_id(大写)。
     */
    public static String extractGuestId(JsonNode reservation) {
        return text(reservation, "guest_id")
                .or(() -> text(reservation, "guestid"))
                .or(() -> text(reservation, "guest_id".toUpperCase(Locale.ROOT)))
                .orElse(null);
    }

    public static String extractSuStatus(JsonNode reservation) {
        return text(reservation, "status").orElse(null);
    }

    public static String extractOtaCode(JsonNode reservation) {
        JsonNode affiliation = reservation != null ? reservation.get("affiliation") : null;
        if (affiliation != null && affiliation.isObject()) {
            return text(affiliation, "OTA_Code")
                    .or(() -> text(affiliation, "ota_code"))
                    .orElse(null);
        }
        return null;
    }

    public static LocalDate extractBookedAt(JsonNode reservation) {
        return parseDate(text(reservation, "booked_at")
                .or(() -> text(reservation, "bookedAt"))
                .orElse(null));
    }

    public static LocalDate extractModifiedAt(JsonNode reservation) {
        return parseDate(text(reservation, "modified_at")
                .or(() -> text(reservation, "modifiedAt"))
                .orElse(null));
    }

    public static String extractPaymentType(JsonNode reservation) {
        return text(reservation, "paymenttype")
                .or(() -> text(reservation, "payment_type"))
                .orElse(null);
    }

    public static String extractCurrencyCode(JsonNode reservation) {
        return text(reservation, "currencycode")
                .or(() -> text(reservation, "currency_code"))
                .orElse(null);
    }

    public static BigDecimal extractCommissionAmount(JsonNode reservation) {
        BigDecimal v = parseBigDecimal(text(reservation, "commissionamount")
                .or(() -> text(reservation, "commission_amount"))
                .orElse(null));
        return v;
    }

    public static String extractCustomerRemarks(JsonNode reservation) {
        JsonNode customer = reservation != null ? reservation.get("customer") : null;
        if (customer != null && customer.isObject()) {
            String remarks = text(customer, "remarks").orElse(null);
            if (remarks != null && !remarks.isBlank()) {
                return remarks.trim();
            }
        }
        return null;
    }

    public static String extractRoomSpecialRequest(JsonNode roomStay) {
        String v = text(roomStay, "specialrequest")
                .or(() -> text(roomStay, "special_request"))
                .orElse(null);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        return null;
    }

    public static String mapOtaChannelCode(String otaCode) {
        if (otaCode == null || otaCode.isBlank()) {
            return null;
        }
        String normalized = otaCode.trim();
        return switch (normalized) {
            case "244" -> "AIRBNB";
            case "19" -> "BOOKING";
            default -> null;
        };
    }

    public static LocalDate extractArrivalDate(JsonNode reservation, JsonNode roomStay) {
        return parseDate(text(roomStay, "arrival_date")
                .or(() -> text(reservation, "arrival_date"))
                .orElse(null));
    }

    public static LocalDate extractDepartureDate(JsonNode reservation, JsonNode roomStay) {
        return parseDate(text(roomStay, "departure_date")
                .or(() -> text(reservation, "departure_date"))
                .orElse(null));
    }

    public static String extractRoomReservationId(JsonNode roomStay) {
        return text(roomStay, "roomreservation_id")
                .orElse(null);
    }
    
    public static String extractRoomStayStatus(JsonNode roomStay) {
        return text(roomStay, "roomstaystatus")
                .or(() -> text(roomStay, "room_stay_status"))
                .orElse(null);
    }

    /**
     * rooms[].id：IT Provider 的 room type ID（实际为你方提供给 Su 的房源/房间标识）。
     * 当前项目推送约定为 roomid={roomTypeId}-{roomNumber}。
     */
    public static String extractRoomTypeId(JsonNode roomStay) {
        return text(roomStay, "id").orElse(null);
    }

    public static String extractGuestName(JsonNode reservation, JsonNode roomStay) {
        String name = text(roomStay, "guest_name").orElse(null);
        if (name != null && !name.isBlank()) {
            return name.trim();
        }

        JsonNode customer = reservation != null ? reservation.get("customer") : null;
        if (customer != null && customer.isObject()) {
            String first = text(customer, "first_name").orElse("");
            String last = text(customer, "last_name").orElse("");
            String full = (first + " " + last).trim();
            if (!full.isBlank()) {
                return full;
            }
        }

        name = text(reservation, "guest_name").orElse(null);
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return "OTA Guest";
    }

    public static String extractGuestPhone(JsonNode reservation, JsonNode roomStay) {
        String phone = text(roomStay, "telephone")
                .or(() -> text(roomStay, "phone"))
                .orElse(null);
        if (phone != null && !phone.isBlank()) {
            return trimPhone(phone);
        }

        JsonNode customer = reservation != null ? reservation.get("customer") : null;
        if (customer != null && customer.isObject()) {
            phone = text(customer, "telephone")
                    .or(() -> text(customer, "phone"))
                    .orElse(null);
            if (phone != null && !phone.isBlank()) {
                return trimPhone(phone);
            }
        }
        return null;
    }

    public static int extractAdults(JsonNode reservation, JsonNode roomStay) {
        Integer v = parseInt(text(roomStay, "numberofadults")
                .or(() -> text(roomStay, "adults"))
                .or(() -> text(reservation, "numberofadults"))
                .orElse(null));
        if (v != null && v > 0) {
            return v;
        }
        v = parseInt(text(roomStay, "numberofguests")
                .or(() -> text(reservation, "numberofguests"))
                .orElse(null));
        if (v != null && v > 0) {
            return v;
        }
        return 1;
    }

    public static int extractChildren(JsonNode reservation, JsonNode roomStay) {
        Integer v = parseInt(text(roomStay, "numberofchildren")
                .or(() -> text(roomStay, "children"))
                .or(() -> text(reservation, "numberofchildren"))
                .orElse(null));
        if (v != null && v >= 0) {
            return v;
        }
        return 0;
    }

    public static BigDecimal extractTotalAmount(JsonNode reservation, JsonNode roomStay) {
        BigDecimal v = parseBigDecimal(text(roomStay, "totalprice")
                .or(() -> text(roomStay, "total_price"))
                .or(() -> text(reservation, "totalprice"))
                .or(() -> text(reservation, "total_price"))
                .orElse(null));
        if (v != null) {
            return v;
        }
        return BigDecimal.ZERO;
    }

    public static String buildOrderNumber(Long storeId, String reservationId, String roomReservationId) {
        String sid = storeId != null ? storeId.toString() : "0";
        String rid = reservationId != null ? reservationId : "unknown";
        String rrid = roomReservationId != null && !roomReservationId.isBlank() ? roomReservationId.trim() : null;

        String base = "SU" + sid + "-" + rid + (rrid != null ? "-" + rrid : "");
        if (base.length() <= 50) {
            return base;
        }
        String hash = Integer.toHexString(base.hashCode());
        String prefix = base.substring(0, Math.min(30, base.length()));
        return prefix + "-" + hash;
    }

    private static Optional<String> text(JsonNode node, String field) {
        if (node == null || node.isNull() || field == null) {
            return Optional.empty();
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return Optional.empty();
        }
        String s = v.asText(null);
        if (s == null) {
            return Optional.empty();
        }
        s = s.trim();
        return s.isBlank() ? Optional.empty() : Optional.of(s);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        // 有些字段可能带时间（YYYY-MM-DD HH:mm:ss），只取日期部分
        if (trimmed.length() >= 10) {
            trimmed = trimmed.substring(0, 10);
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimPhone(String raw) {
        String cleaned = raw.trim().replaceAll("[^0-9+]", "");
        return cleaned.isBlank() ? null : cleaned;
    }
}
