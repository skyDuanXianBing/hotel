package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            hotelId = SuHotelIdUtil.buildDefault(storeId);
            store.setSuHotelId(hotelId);
            storeRepository.save(store);
        }

        // 使用缓存的 Su Access Token（自动复用/刷新）

        Map<String, Object> payload = buildPropertyPayload(store, hotelId);

        try {
            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.upsertProperty(token, payload),
                    "OTA_HotelDescriptiveContentNotif"
            );
            boolean ok = suApiClient.isSuSuccess(response);
            if (ok) {
                return new UpsertResult(true, true, hotelId, "Su 物业创建/覆盖成功");
            }
            String err = suApiClient.extractSuErrorMessage(response);
            return new UpsertResult(true, false, hotelId, err != null ? err : "Su 返回失败");
        } catch (Exception e) {
            return new UpsertResult(true, false, hotelId, e.getMessage());
        }
    }

    private static Map<String, Object> buildPropertyPayload(Store store, String hotelId) {
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
        content.put("HotelDescriptiveContentNotifType", "New");
        content.put("HotelInfo", hotelInfo);
        content.put("ContactInfos", Map.of("ContactInfo", List.of(physicalLocationContact, availabilityContact)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("HotelDescriptiveContents", Map.of("HotelDescriptiveContent", content));
        return body;
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
