package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.FacilityDTO;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Su Property（hotelid/HotelCode）创建/覆盖服务。
 */
@Service
public class SuPropertyService {

    private final StoreRepository storeRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public record UpsertResult(
            boolean attempted,
            boolean success,
            String hotelId,
            String message
    ) {}

    public record RemoveResult(
            boolean attempted,
            boolean success,
            String hotelId,
            String errorCode,
            String message
    ) {}

    public SuPropertyService(StoreRepository storeRepository, SuApiClient suApiClient, SuAccessTokenService suAccessTokenService) {
        this.storeRepository = storeRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    @Transactional
    public UpsertResult upsertStoreProperty(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));

        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null) {
            hotelId = generateUniqueRandomSuHotelId();
            store.setSuHotelId(hotelId);
            storeRepository.save(store);
        }

        // 使用缓存的 Su Access Token（自动复用/刷新）

        Map<String, Object> payload = buildPropertyPayload(store, hotelId, "New");

        try {
            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.upsertProperty(token, payload),
                    "OTA_HotelDescriptiveContentNotif"
            );
            boolean ok = suApiClient.isSuSuccess(response);
            if (ok) {
                return new UpsertResult(true, true, hotelId, "渠道物业创建成功");
            }
            String err = suApiClient.extractSuErrorMessage(response);
            if (isPropertyAlreadyExists(err, response) || isAccessDenied(err, response)) {
                return new UpsertResult(true, false, hotelId,
                        "渠道物业创建失败：该酒店ID可能已被占用或不归属当前账号，请更换后重试。原始错误："
                                + (err != null ? err : "未知"));
            }
            if (false && isPropertyAlreadyExists(err, response)) {
                Map<String, Object> overlayPayload = buildPropertyPayload(store, hotelId, "Overlay");
                JsonNode overlayResp = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.upsertProperty(token, overlayPayload),
                        "OTA_HotelDescriptiveContentNotif(Overlay)"
                );
                if (suApiClient.isSuSuccess(overlayResp)) {
                    return new UpsertResult(true, true, hotelId, "渠道物业已存在，覆盖更新成功");
                }
                String overlayErr = suApiClient.extractSuErrorMessage(overlayResp);
                return new UpsertResult(true, false, hotelId, overlayErr != null ? overlayErr : "渠道接口返回失败");
            }
            return new UpsertResult(true, false, hotelId, err != null ? err : "渠道接口返回失败");
        } catch (Exception e) {
            return new UpsertResult(true, false, hotelId, e.getMessage());
        }
    }

    /**
     * 删除 Su Property（RemoveProperty）。
     * <p>
     * 注意：Su 可能因为该 Property 仍有渠道映射而返回 953（This Property Have Mapping With Channels）。
     */
    @Transactional
    public RemoveResult removeStoreProperty(Long storeId, boolean forceDeletion) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null || hotelId.isBlank()) {
            return new RemoveResult(false, false, null, null, "Su hotelId 未配置，无法删除 Su Property");
        }

        try {
            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.removeProperty(token, hotelId, forceDeletion),
                    "RemoveProperty"
            );
            boolean ok = suApiClient.isSuSuccess(response);
            if (ok) {
                return new RemoveResult(true, true, hotelId, null, "Su Property 删除成功");
            }
            String err = suApiClient.extractSuErrorMessage(response);
            String code = suApiClient.extractSuErrorCode(response);
            String msg = err != null && !err.isBlank() ? err : "Su Property 删除失败";
            return new RemoveResult(true, false, hotelId, code, msg);
        } catch (Exception e) {
            return new RemoveResult(true, false, hotelId, null, e.getMessage());
        }
    }

    private static Map<String, Object> buildPropertyPayload(Store store, String hotelId, String notifType) {
        String currency = store.getCurrency() != null && !store.getCurrency().isBlank()
                ? store.getCurrency()
                : "CNY";
        String timezone = store.getTimezone() != null && !store.getTimezone().isBlank()
                ? store.getTimezone()
                : "Asia/Shanghai";

        String email = store.getEmail() != null && !store.getEmail().isBlank()
                ? store.getEmail()
                : store.getOwnerEmail();

        Map<String, Object> position = new LinkedHashMap<>();
        position.put("Latitude", "0");
        position.put("Longitude", "0");

        Map<String, Object> hotelInfo = new LinkedHashMap<>();
        hotelInfo.put("Position", position);

        String countryCode = toCountryCode(store.getCountry());

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("AddressLine", store.getAddress() != null && !store.getAddress().isBlank() ? store.getAddress() : "N/A");
        address.put("CityName", store.getCity() != null && !store.getCity().isBlank() ? store.getCity() : "N/A");
        address.put("PostalCode", "000000");
        address.put("CountryName", countryCode);
        if (store.getState() != null && !store.getState().isBlank()) {
            address.put("StateProv", store.getState());
        }

        Map<String, Object> phone = new LinkedHashMap<>();
        String phoneNumber = normalizePhone(store.getPhone());
        if (phoneNumber == null) {
            phoneNumber = "+8610000000000";
        }
        if (phoneNumber != null) {
            phone.put("PhoneNumber", phoneNumber);
            phone.put("PhoneTechType", "1");
        }
        Map<String, Object> phones = new LinkedHashMap<>();
        if (!phone.isEmpty()) {
            phones.put("Phone", List.of(phone));
        }

        Map<String, Object> physicalLocationContact = new LinkedHashMap<>();
        physicalLocationContact.put("ContactProfileType", "PhysicalLocation");
        physicalLocationContact.put("Addresses", Map.of("Address", address));

        Map<String, Object> availabilityContact = new LinkedHashMap<>();
        availabilityContact.put("ContactProfileType", "availability");
        availabilityContact.put("Addresses", Map.of("Address", address));

        String givenName = "HostHub";
        String surname = "Admin";
        if (store.getManager() != null && !store.getManager().isBlank()) {
            String[] parts = store.getManager().trim().split("\\s+", 2);
            givenName = parts[0];
            if (parts.length > 1) {
                surname = parts[1];
            }
        }
        availabilityContact.put("Names", Map.of("Name", Map.of("GivenName", givenName, "Surname", surname)));

        if (email != null && !email.isBlank()) {
            availabilityContact.put("Emails", Map.of("Email", List.of(email)));
        }
        if (!phones.isEmpty()) {
            availabilityContact.put("Phones", phones);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("HotelName", store.getName());
        content.put("HotelType", "3");
        content.put("TimeZone", timezone);
        content.put("Platform", "SU");
        content.put("hotelid", hotelId);
        content.put("LanguageCode", "en");
        content.put("CurrencyCode", currency);
        content.put("HotelDescriptiveContentNotifType", notifType != null && !notifType.isBlank() ? notifType : "New");
        content.put("HotelInfo", hotelInfo);
        content.put("ContactInfos", Map.of("ContactInfo", List.of(physicalLocationContact, availabilityContact)));
        if (store.getDescription() != null && !store.getDescription().isBlank()) {
            content.put("HotelDescription", store.getDescription().trim());
        }
        Map<String, Object> facilities = buildFacilitiesNode(store.getFacilities());
        if (!facilities.isEmpty()) {
            content.put("Facilities", facilities);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("HotelDescriptiveContents", Map.of("HotelDescriptiveContent", content));
        return body;
    }

    private static Map<String, Object> buildFacilitiesNode(List<FacilityDTO> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> facilityItems = facilities.stream()
                .filter(item -> item != null && item.getName() != null && !item.getName().isBlank())
                .map(item -> {
                    Map<String, Object> facility = new LinkedHashMap<>();
                    facility.put("Group", item.getGroup() != null && !item.getGroup().isBlank() ? item.getGroup().trim() : "Common");
                    facility.put("name", item.getName().trim());
                    return facility;
                })
                .toList();
        if (facilityItems.isEmpty()) {
            return Map.of();
        }
        return Map.of("Facility", facilityItems);
    }

    private static boolean isPropertyAlreadyExists(String err, JsonNode response) {
        String msg = err;
        if (msg == null && response != null) {
            msg = response.toString();
        }
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("property") && lower.contains("already") && lower.contains("exist");
    }

    private static boolean isAccessDenied(String err, JsonNode response) {
        String msg = err;
        if (msg == null && response != null) {
            msg = response.toString();
        }
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("access denied") || lower.contains("authorization error");
    }

    private String generateUniqueRandomSuHotelId() {
        for (int i = 0; i < 50; i++) {
            String candidate = SuHotelIdUtil.generateRandom();
            if (storeRepository.findBySuHotelId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new RuntimeException("渠道酒店ID生成失败，请手动填写");
    }

    private static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9+]", "");
        if (cleaned.isBlank()) {
            return null;
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned.replaceAll("[^0-9]", "");
        }
        return cleaned;
    }

    private static String toCountryCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CN";
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 2) {
            return trimmed.toUpperCase();
        }
        return switch (trimmed.toLowerCase()) {
            case "china", "中国" -> "CN";
            case "japan", "日本" -> "JP";
            case "usa", "united states", "美国" -> "US";
            case "uk", "united kingdom", "英国" -> "GB";
            default -> "CN";
        };
    }
}
