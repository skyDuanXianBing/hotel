package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.FacilityDTO;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.repository.StoreRepository;
import server.demo.repository.StorePolicyRepository;
import server.demo.util.SuHotelIdUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuPropertyService {

    private static final String NOTIF_TYPE_NEW = "New";
    private static final String NOTIF_TYPE_OVERLAY = "Overlay";

    private final StoreRepository storeRepository;
    private final StorePolicyRepository storePolicyRepository;
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

    public static boolean isPropertyAlreadyMissing(RemoveResult result) {
        if (result == null) {
            return false;
        }

        String code = result.errorCode();
        if (code != null) {
            String normalizedCode = code.trim().toLowerCase();
            if (normalizedCode.contains("invalid hotelcode") || normalizedCode.contains("invalid hotel code")) {
                return true;
            }
        }

        String message = result.message();
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalizedMessage = message.trim().toLowerCase();
        return normalizedMessage.contains("invalid hotelcode")
                || normalizedMessage.contains("invalid hotel code")
                || normalizedMessage.contains("hotel not found")
                || normalizedMessage.contains("property not found")
                || normalizedMessage.contains("unknown hotelcode");
    }

    public SuPropertyService(
            StoreRepository storeRepository,
            StorePolicyRepository storePolicyRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.storeRepository = storeRepository;
        this.storePolicyRepository = storePolicyRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    @Transactional
    public UpsertResult upsertStoreProperty(Long storeId) {
        return submitStoreProperty(storeId, NOTIF_TYPE_NEW);
    }

    @Transactional
    public UpsertResult updateStoreProperty(Long storeId) {
        UpsertResult overlayResult = submitStoreProperty(storeId, NOTIF_TYPE_OVERLAY);
        if (overlayResult.success()) {
            return overlayResult;
        }
        if (!isPropertyMissingForOverlay(overlayResult.message())) {
            return overlayResult;
        }
        return submitStoreProperty(storeId, NOTIF_TYPE_NEW);
    }

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

    private UpsertResult submitStoreProperty(Long storeId, String notifType) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null) {
            hotelId = generateUniqueRandomSuHotelId();
            store.setSuHotelId(hotelId);
            storeRepository.save(store);
        }

        StorePolicy storePolicy = storePolicyRepository.findByStoreId(storeId).orElse(null);
        Map<String, Object> payload = buildPropertyPayload(store, storePolicy, hotelId, notifType);

        try {
            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.upsertProperty(token, payload),
                    "OTA_HotelDescriptiveContentNotif(" + notifType + ")"
            );
            if (suApiClient.isSuSuccess(response)) {
                return new UpsertResult(true, true, hotelId, buildSuccessMessage(notifType));
            }

            String errorMessage = suApiClient.extractSuErrorMessage(response);
            if (NOTIF_TYPE_NEW.equalsIgnoreCase(notifType) && (isPropertyAlreadyExists(errorMessage, response) || isAccessDenied(errorMessage, response))) {
                return new UpsertResult(
                        true,
                        false,
                        hotelId,
                        "渠道物业创建失败：该酒店ID可能已被占用或不归属当前账号，请更换后重试。原始错误："
                                + (errorMessage != null ? errorMessage : "unknown")
                );
            }

            return new UpsertResult(true, false, hotelId, errorMessage != null ? errorMessage : "Su Property 同步失败");
        } catch (Exception e) {
            return new UpsertResult(true, false, hotelId, e.getMessage());
        }
    }

    private static String buildSuccessMessage(String notifType) {
        if (NOTIF_TYPE_OVERLAY.equalsIgnoreCase(notifType)) {
            return "Su property updated successfully";
        }
        return "Su property created successfully";
    }

    private static Map<String, Object> buildPropertyPayload(Store store, StorePolicy storePolicy, String hotelId, String notifType) {
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
            phone.put("PhoneTechType", normalizePhoneTechType(store.getPhoneTechType()));
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
        content.put("HotelType", normalizeHotelType(store.getType()));
        content.put("TimeZone", timezone);
        content.put("Platform", "SU");
        content.put("hotelid", hotelId);
        content.put("LanguageCode", normalizeLanguageCode(store.getLanguage()));
        content.put("CurrencyCode", currency);
        content.put("HotelDescriptiveContentNotifType", notifType != null && !notifType.isBlank() ? notifType : NOTIF_TYPE_NEW);
        if (storePolicy != null && storePolicy.getCheckinTime() != null && !storePolicy.getCheckinTime().isBlank()) {
            content.put("OfficialCheckinTime", storePolicy.getCheckinTime().trim());
        }
        if (storePolicy != null && storePolicy.getCheckoutTime() != null && !storePolicy.getCheckoutTime().isBlank()) {
            content.put("OfficialCheckoutTime", storePolicy.getCheckoutTime().trim());
        }
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
        String message = err;
        if (message == null && response != null) {
            message = response.toString();
        }
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("property") && lower.contains("already") && lower.contains("exist");
    }

    private static boolean isAccessDenied(String err, JsonNode response) {
        String message = err;
        if (message == null && response != null) {
            message = response.toString();
        }
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("access denied") || lower.contains("authorization error");
    }

    private static boolean isPropertyMissingForOverlay(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("invalid hotelcode")
                || lower.contains("invalid hotel code")
                || lower.contains("hotel not found")
                || lower.contains("property not found")
                || lower.contains("unknown hotelcode")
                || lower.contains("not found");
    }

    private static String normalizeHotelType(String value) {
        if (value == null || value.isBlank()) {
            return "1";
        }
        String normalizedValue = value.trim().toLowerCase();
        return switch (normalizedValue) {
            case "1", "hotel" -> "1";
            case "2", "motel" -> "2";
            case "3", "vacational rental", "vacation rental", "vacational_rental", "homestay", "apartment", "hostel" -> "3";
            default -> "1";
        };
    }

    private static String normalizeLanguageCode(String value) {
        if (value == null || value.isBlank()) {
            return "en";
        }
        String normalizedValue = value.trim().toLowerCase();
        return switch (normalizedValue) {
            case "en", "english" -> "en";
            case "zh", "zh-cn", "zh_cn", "chinese", "simplified chinese", "简体中文", "中文" -> "zh";
            case "ja", "jp", "japanese", "日本語", "日语", "日文" -> "ja";
            default -> "en";
        };
    }

    private static String normalizePhoneTechType(String value) {
        if (value == null || value.isBlank()) {
            return "1";
        }
        String normalizedValue = value.trim().toLowerCase();
        return switch (normalizedValue) {
            case "1", "voice", "landline", "fixed", "phone", "固定电话", "固话" -> "1";
            case "3", "fax", "传真" -> "3";
            case "5", "mobile", "cell", "cellphone", "手机" -> "5";
            default -> "1";
        };
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
