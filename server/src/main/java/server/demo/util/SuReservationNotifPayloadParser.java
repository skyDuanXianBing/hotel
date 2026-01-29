package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 解析 Su 预订通知 Push 的 payload（reservation_notif_id 列表）。
 *
 * 文档参考：docs/预订/预订通知推送.txt
 */
public final class SuReservationNotifPayloadParser {

    private SuReservationNotifPayloadParser() {}

    public static List<String> extractNotifIds(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }

        JsonNode reservationNotif = root.get("reservation_notif");
        if (reservationNotif == null) {
            reservationNotif = root.get("reservation_notif".toUpperCase(Locale.ROOT));
        }
        if (reservationNotif == null || reservationNotif.isNull()) {
            // Fallback: PUSH API Method may deliver reservation details directly
            // in a `reservations` array (each reservation contains `reservation_notif_id`).
            return extractNotifIdsFromReservationsArray(root);
        }

        JsonNode ids = reservationNotif.get("reservation_notif_id");
        if (ids == null) {
            ids = reservationNotif.get("reservation_notif_id".toUpperCase(Locale.ROOT));
        }
        if (ids == null || ids.isNull()) {
            return extractNotifIdsFromReservationsArray(root);
        }

        Set<String> dedup = new LinkedHashSet<>();
        if (ids.isArray()) {
            for (JsonNode idNode : ids) {
                if (idNode == null || idNode.isNull()) {
                    continue;
                }
                String id = idNode.asText(null);
                if (id == null) {
                    continue;
                }
                id = id.trim();
                if (!id.isBlank()) {
                    dedup.add(id);
                }
            }
        } else {
            String id = ids.asText(null);
            if (id != null && !id.trim().isBlank()) {
                dedup.add(id.trim());
            }
        }

        return new ArrayList<>(dedup);
    }

    private static List<String> extractNotifIdsFromReservationsArray(JsonNode root) {
        JsonNode reservations = root != null ? root.get("reservations") : null;
        if (reservations == null || !reservations.isArray()) {
            return List.of();
        }

        Set<String> dedup = new LinkedHashSet<>();
        for (JsonNode item : reservations) {
            if (item == null || item.isNull()) {
                continue;
            }
            JsonNode reservation = item.get("reservation");
            JsonNode node = (reservation != null && reservation.isObject()) ? reservation : item;

            JsonNode idNode = node.get("reservation_notif_id");
            if (idNode == null) {
                idNode = node.get("reservation_notif_id".toUpperCase(Locale.ROOT));
            }
            if (idNode == null || idNode.isNull()) {
                continue;
            }
            String id = idNode.asText(null);
            if (id == null) {
                continue;
            }
            id = id.trim();
            if (!id.isBlank()) {
                dedup.add(id);
            }
        }

        return dedup.isEmpty() ? List.of() : new ArrayList<>(dedup);
    }
}

