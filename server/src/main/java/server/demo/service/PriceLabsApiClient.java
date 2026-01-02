package server.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import server.demo.config.PriceLabsConfig;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PriceLabsApiClient {
    private static final Logger logger = LoggerFactory.getLogger(PriceLabsApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private PriceLabsConfig priceLabsConfig;
    @Autowired private RestTemplate priceLabsRestTemplate;

    private volatile String runtimeIntegrationTokenOverride;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String integrationName = priceLabsConfig.getIntegrationName();
        if (integrationName != null) integrationName = integrationName.trim();
        String integrationToken = runtimeIntegrationTokenOverride != null ? runtimeIntegrationTokenOverride : priceLabsConfig.getIntegrationToken();
        if (integrationToken != null) integrationToken = integrationToken.trim();

        logger.info("Using PriceLabs Integration Name: {}", integrationName);
        logger.info("Using PriceLabs Integration Token: {} (length: {}, masked: {})",
            integrationToken != null ? "CONFIGURED" : "NULL",
            integrationToken != null ? integrationToken.length() : 0,
            maskToken(integrationToken));

        if (integrationName == null || integrationName.isEmpty()) {
            throw new IllegalStateException("PRICELABS_INTEGRATION_NAME not configured");
        }
        if (integrationToken == null || integrationToken.isEmpty()) {
            throw new IllegalStateException("PRICELABS_INTEGRATION_TOKEN not configured");
        }

        headers.set("X-INTEGRATION-NAME", integrationName);
        headers.set("X-INTEGRATION-TOKEN", integrationToken);
        return headers;
    }

    private static String maskToken(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.length() <= 8) return "****";
        return t.substring(0, 4) + "..." + t.substring(t.length() - 4);
    }

    public static class ListingData {
        @JsonProperty("listing_id")
        private String listingId;
        @JsonProperty("user_token")
        private String userToken;
        private Location location;
        private String name;
        private String status;
        private String address;
        private String city;
        private String state;
        private String country;
        private String timezone;
        private Double latitude;
        private Double longitude;
        @JsonProperty("number_of_bedrooms")
        private Integer bedrooms;
        private Double bathrooms;
        private Integer accommodates;
        @JsonProperty("property_type")
        private String propertyType;
        private String currency;
        @JsonProperty("base_price")
        private BigDecimal basePrice;
        @JsonProperty("min_price")
        private BigDecimal minPrice;
        @JsonProperty("max_price")
        private BigDecimal maxPrice;
        private Boolean active;
        @JsonProperty("multi_unit")
        private Boolean multiUnit;
        @JsonProperty("multi_unit_count")
        private Integer multiUnitCount;

        public String getListingId() { return listingId; }
        public void setListingId(String v) { this.listingId = v; }
        public String getUserToken() { return userToken; }
        public void setUserToken(String v) { this.userToken = v; }
        public Location getLocation() { return location; }
        public void setLocation(Location v) { this.location = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getAddress() { return address; }
        public void setAddress(String v) { this.address = v; }
        public String getCity() { return city; }
        public void setCity(String v) { this.city = v; }
        public String getState() { return state; }
        public void setState(String v) { this.state = v; }
        public String getCountry() { return country; }
        public void setCountry(String v) { this.country = v; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String v) { this.timezone = v; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double v) { this.latitude = v; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double v) { this.longitude = v; }
        public Integer getBedrooms() { return bedrooms; }
        public void setBedrooms(Integer v) { this.bedrooms = v; }
        public Double getBathrooms() { return bathrooms; }
        public void setBathrooms(Double v) { this.bathrooms = v; }
        public Integer getAccommodates() { return accommodates; }
        public void setAccommodates(Integer v) { this.accommodates = v; }
        public String getPropertyType() { return propertyType; }
        public void setPropertyType(String v) { this.propertyType = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { this.currency = v; }
        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal v) { this.basePrice = v; }
        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal v) { this.minPrice = v; }
        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal v) { this.maxPrice = v; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean v) { this.active = v; }
        public Boolean getMultiUnit() { return multiUnit; }
        public void setMultiUnit(Boolean v) { this.multiUnit = v; }
        public Integer getMultiUnitCount() { return multiUnitCount; }
        public void setMultiUnitCount(Integer v) { this.multiUnitCount = v; }
    }

    public static class Location {
        private Double latitude;
        private Double longitude;
        private String city;
        private String country;

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double v) { this.latitude = v; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double v) { this.longitude = v; }
        public String getCity() { return city; }
        public void setCity(String v) { this.city = v; }
        public String getCountry() { return country; }
        public void setCountry(String v) { this.country = v; }
    }

    public static class RatePlanData {
        @JsonProperty("listing_id")
        private String listingId;
        @JsonProperty("rate_plan_id")
        private String ratePlanId;
        private String name;
        @JsonProperty("is_default")
        private Boolean isDefault;
        @JsonProperty("occupancy_based")
        private Boolean occupancyBased;

        public String getListingId() { return listingId; }
        public void setListingId(String v) { this.listingId = v; }
        public String getRatePlanId() { return ratePlanId; }
        public void setRatePlanId(String v) { this.ratePlanId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean v) { this.isDefault = v; }
        public Boolean getOccupancyBased() { return occupancyBased; }
        public void setOccupancyBased(Boolean v) { this.occupancyBased = v; }
    }

    public static class RatePlansByListing {
        @JsonProperty("listing_id")
        private String listingId;
        private List<RatePlanItem> data;

        public String getListingId() { return listingId; }
        public void setListingId(String v) { this.listingId = v; }
        public List<RatePlanItem> getData() { return data; }
        public void setData(List<RatePlanItem> v) { this.data = v; }
    }

    public static class RatePlanItem {
        private String id;
        private String name;
        @JsonProperty("default")
        private String defaultFlag;

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getDefaultFlag() { return defaultFlag; }
        public void setDefaultFlag(String v) { this.defaultFlag = v; }
    }

    public static class CalendarEntry {
        private String date;
        @JsonProperty("end_date")
        private String endDate;
        private BigDecimal price;
        @JsonProperty("available_units")
        private Integer availableUnits;
        @JsonProperty("booked_units")
        private Integer bookedUnits;
        @JsonProperty("blocked_units")
        private Integer blockedUnits;
        private CalendarSettings settings;

        // 旧字段：用于内部计算/兼容，但不向 PriceLabs 发送（PriceLabs /calendar 只认 units + settings）
        @JsonIgnore
        private Boolean available;
        @JsonIgnore
        private Integer minStay;

        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String v) { this.endDate = v; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal v) { this.price = v; }
        public Integer getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(Integer v) { this.availableUnits = v; }
        public Integer getBookedUnits() { return bookedUnits; }
        public void setBookedUnits(Integer v) { this.bookedUnits = v; }
        public Integer getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(Integer v) { this.blockedUnits = v; }
        public CalendarSettings getSettings() { return settings; }
        public void setSettings(CalendarSettings v) { this.settings = v; }

        @JsonIgnore
        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean v) { this.available = v; }
        @JsonIgnore
        public Integer getMinStay() { return minStay; }
        public void setMinStay(Integer v) { this.minStay = v; }
    }

    public static class CalendarSettings {
        @JsonProperty("min_stay")
        private Integer minStay;
        @JsonProperty("check_in")
        private Boolean checkIn;
        @JsonProperty("check_out")
        private Boolean checkOut;

        public Integer getMinStay() { return minStay; }
        public void setMinStay(Integer v) { this.minStay = v; }
        public Boolean getCheckIn() { return checkIn; }
        public void setCheckIn(Boolean v) { this.checkIn = v; }
        public Boolean getCheckOut() { return checkOut; }
        public void setCheckOut(Boolean v) { this.checkOut = v; }
    }

    public static class CalendarData {
        @JsonProperty("listing_id")
        private String listingId;
        @JsonProperty("rate_plan_id")
        private String ratePlanId;
        private String currency;
        private List<CalendarEntry> calendar;

        public String getListingId() { return listingId; }
        public void setListingId(String v) { this.listingId = v; }
        public String getRatePlanId() { return ratePlanId; }
        public void setRatePlanId(String v) { this.ratePlanId = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { this.currency = v; }

        // PriceLabs /calendar 只接受 data 字段；内部仍沿用 calendar 命名，避免大范围改动业务代码
        @JsonProperty("data")
        @com.fasterxml.jackson.annotation.JsonAlias({"calendar"})
        public List<CalendarEntry> getData() { return calendar; }

        @JsonProperty("data")
        public void setData(List<CalendarEntry> v) { this.calendar = v; }

        @JsonIgnore
        public List<CalendarEntry> getCalendar() { return calendar; }
        @JsonIgnore
        public void setCalendar(List<CalendarEntry> v) { this.calendar = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceLabsResponse {
        @JsonProperty("success")
        @com.fasterxml.jackson.annotation.JsonAlias({"successes"})
        private List<Object> success;

        @JsonProperty("failure")
        @com.fasterxml.jackson.annotation.JsonAlias({"errors", "failed"})
        private List<Object> failure;

        public List<Object> getSuccess() { return success; }
        public void setSuccess(List<Object> v) { this.success = v; }
        public List<Object> getFailure() { return failure; }
        public void setFailure(List<Object> v) { this.failure = v; }
    }

    private PriceLabsResponse postForPriceLabsResponse(String url, HttpEntity<?> req) {
        try {
            ResponseEntity<String> rawRes = priceLabsRestTemplate.exchange(url, HttpMethod.POST, req, String.class);
            String rawBody = rawRes.getBody();
            if (rawBody == null || rawBody.trim().isEmpty()) return new PriceLabsResponse();
            return objectMapper.readValue(rawBody, PriceLabsResponse.class);
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            String snippet = body == null ? "" : body.substring(0, Math.min(500, body.length()));
            throw new RuntimeException("HTTP " + e.getStatusCode().value() + " " + e.getStatusText() + (snippet.isEmpty() ? "" : (": " + snippet)), e);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("Parse PriceLabs response failed: " + msg, e);
        }
    }

    public PriceLabsResponse pushListings(List<ListingData> listings) {
        String url = priceLabsConfig.getBaseUrl() + "/listings";
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("listings", listings), headers);
        try {
            logger.info("Push {} listings to PriceLabs", listings.size());
            PriceLabsResponse r = postForPriceLabsResponse(url, req);
            if (r != null) {
                logger.info("Listing push ok. Success: {}, Fail: {}",
                    r.getSuccess() != null ? r.getSuccess().size() : 0,
                    r.getFailure() != null ? r.getFailure().size() : 0);
            }
            return r;
        } catch (Exception e) {
            logger.error("Push listings failed", e);
            throw new RuntimeException("Push listings failed: " + e.getMessage(), e);
        }
    }

    public PriceLabsResponse pushRatePlans(List<RatePlanData> plans) {
        String url = priceLabsConfig.getBaseUrl() + "/rate_plans";
        HttpHeaders headers = createHeaders();
        List<RatePlansByListing> payload = buildRatePlansPayload(plans);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("rate_plans", payload), headers);
        try {
            logger.info("Push {} rate plans to PriceLabs", plans.size());
            PriceLabsResponse r = postForPriceLabsResponse(url, req);
            if (r != null) {
                logger.info("Rate plans push ok. Success: {}, Fail: {}",
                    r.getSuccess() != null ? r.getSuccess().size() : 0,
                    r.getFailure() != null ? r.getFailure().size() : 0);
            }
            return r;
        } catch (Exception e) {
            logger.error("Push rate plans failed", e);
            throw new RuntimeException("Push rate plans failed: " + e.getMessage(), e);
        }
    }

    static List<RatePlansByListing> buildRatePlansPayload(List<RatePlanData> plans) {
        Map<String, List<RatePlanItem>> grouped = new java.util.LinkedHashMap<>();
        if (plans != null) {
            for (RatePlanData p : plans) {
                if (p == null) continue;
                String listingId = p.getListingId();
                if (listingId == null || listingId.isBlank()) continue;

                String ratePlanId = p.getRatePlanId();
                String name = p.getName();
                if (ratePlanId == null || ratePlanId.isBlank() || name == null || name.isBlank()) {
                    continue;
                }

                grouped.computeIfAbsent(listingId, k -> new ArrayList<>());

                RatePlanItem item = new RatePlanItem();
                item.setId(ratePlanId);
                item.setName(name);
                item.setDefaultFlag(Boolean.TRUE.equals(p.getIsDefault()) ? "true" : "false");
                grouped.get(listingId).add(item);
            }
        }

        List<RatePlansByListing> payload = new ArrayList<>();
        for (Map.Entry<String, List<RatePlanItem>> e : grouped.entrySet()) {
            RatePlansByListing row = new RatePlansByListing();
            row.setListingId(e.getKey());
            row.setData(e.getValue());
            payload.add(row);
        }
        return payload;
    }

    public PriceLabsResponse pushCalendar(List<CalendarData> calendars) {
        String url = priceLabsConfig.getBaseUrl() + "/calendar";
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("calendars", calendars), headers);
        try {
            logger.info("Push {} calendars to PriceLabs", calendars.size());
            PriceLabsResponse r = postForPriceLabsResponse(url, req);
            if (r != null) {
                logger.info("Calendar push ok. Success: {}, Fail: {}",
                    r.getSuccess() != null ? r.getSuccess().size() : 0,
                    r.getFailure() != null ? r.getFailure().size() : 0);
                if (r.getFailure() != null && !r.getFailure().isEmpty()) {
                    logger.warn("PriceLabs /calendar returned failure sample: {}", toJsonSnippet(r.getFailure().get(0), 800));
                }
            }
            return r;
        } catch (Exception e) {
            logger.error("Push calendar failed", e);
            throw new RuntimeException("Push calendar failed: " + e.getMessage(), e);
        }
    }

    private static String toJsonSnippet(Object value, int maxLen) {
        if (value == null) return "null";
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() <= maxLen) return json;
            return json.substring(0, maxLen) + "...";
        } catch (Exception e) {
            String s = String.valueOf(value);
            if (s.length() <= maxLen) return s;
            return s.substring(0, maxLen) + "...";
        }
    }

    public Map<String, Object> registerIntegration() {
        // 构建URL
        String url = priceLabsConfig.getBaseUrl() + "/integration";

        HttpHeaders headers = createHeaders();

        Map<String, Object> features = new java.util.HashMap<>();
        // 以 Swagger 为准：features/feature_access 仅允许这些 key（其它 key 会导致 /integration 返回 failure）
        features.put("min_stay", true);
        features.put("check_in", false);
        features.put("check_out", false);
        features.put("monthly_weekly_discounts", false);
        features.put("extra_person_fee", false);
        features.put("los_pricing", false);
        features.put("delta_only", priceLabsConfig.isDeltaOnly());

        // 构建请求体
        Map<String, Object> integration = new java.util.HashMap<>();
        integration.put("sync_url", priceLabsConfig.getSyncUrl());
        integration.put("calendar_trigger_url", priceLabsConfig.getCalendarTriggerUrl());
        integration.put("hook_url", priceLabsConfig.getHookUrl());
        integration.put("regenerate_token", priceLabsConfig.isRegenerateToken());
        integration.put("features", features);
        // PriceLabs 的错误信息里也会使用 feature_access 的字段名，这里同时兼容写入
        integration.put("feature_access", features);

        Map<String, Object> body = Map.of("integration", integration);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            assertWebhookUrlReachable(priceLabsConfig.getSyncUrl(), "sync_url");
            assertWebhookUrlReachable(priceLabsConfig.getCalendarTriggerUrl(), "calendar_trigger_url");
            assertWebhookUrlReachable(priceLabsConfig.getHookUrl(), "hook_url");

            logger.info("Register integration with PriceLabs: sync_url={}, calendar_trigger_url={}, hook_url={}, features={}",
                priceLabsConfig.getSyncUrl(), priceLabsConfig.getCalendarTriggerUrl(), priceLabsConfig.getHookUrl(), features);

            ResponseEntity<String> rawRes = priceLabsRestTemplate.exchange(url, HttpMethod.POST, req, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(rawRes.getBody() == null ? "{}" : rawRes.getBody(), Map.class);

            List<String> failures = extractFailureMessages(response);
            if (!failures.isEmpty()) {
                logger.warn("PriceLabs integration registration returned failures: {}", failures);
                throw new RuntimeException("Register integration failed: " + String.join("; ", failures));
            }

            // 处理新token(如果regenerate_token=true)
            if (priceLabsConfig.isRegenerateToken()) {
                String newToken = extractNewToken(response);
                if (newToken != null && !newToken.isBlank()) {
                    runtimeIntegrationTokenOverride = newToken.trim();
                    logger.warn("PriceLabs returned a new integration token (masked={} len={}). Please update your configuration/env to persist it.",
                        maskToken(newToken), newToken.trim().length());
                } else {
                    logger.info("regenerate_token=true but no token field in response");
                }
            }

            logger.info("PriceLabs integration registered successfully. Response: {}", response);
            return response;
        } catch (HttpStatusCodeException e) {
            String bodyText = e.getResponseBodyAsString();
            String snippet = bodyText == null ? "" : bodyText.substring(0, Math.min(500, bodyText.length()));
            logger.error("Register integration failed: status={}, body={}", e.getStatusCode().value(), snippet);

            int status = e.getStatusCode().value();
            StringBuilder message = new StringBuilder();
            message.append("Register integration failed: HTTP ").append(status);

            if (status == 401) {
                message.append(". Please verify PRICELABS_INTEGRATION_NAME / PRICELABS_INTEGRATION_TOKEN (token may have been reset in PriceLabs console)");
            } else if (status >= 500) {
                message.append(". PriceLabs server error. If problem persists, contact support@pricelabs.co and provide the error_code/message from response");
            }

            if (!snippet.isEmpty()) {
                message.append(". Response: ").append(snippet);
            }

            throw new RuntimeException(message.toString(), e);
        } catch (Exception e) {
            logger.error("Register integration failed", e);
            throw new RuntimeException("Register integration failed: " + e.getMessage(), e);
        }
    }

    private static void assertWebhookUrlReachable(String url, String name) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Webhook URL 未配置: " + name);
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        int status = headThenGetStatus(client, url);
        if (status >= 400) {
            throw new IllegalStateException("Webhook URL 不可访问: " + name + "=" + url + " (HTTP " + status + ")");
        }
    }

    private static int headThenGetStatus(HttpClient client, String url) {
        try {
            URI uri = URI.create(url);
            HttpRequest head = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<Void> headRes = client.send(head, HttpResponse.BodyHandlers.discarding());
            int status = headRes.statusCode();
            if (status != 405) return status;

            HttpRequest get = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

            HttpResponse<Void> getRes = client.send(get, HttpResponse.BodyHandlers.discarding());
            return getRes.statusCode();
        } catch (Exception e) {
            throw new IllegalStateException("Webhook URL 访问失败: " + url + " (" + e.getMessage() + ")", e);
        }
    }

    static List<String> extractFailureMessages(Map<String, Object> response) {
        List<String> failures = new ArrayList<>();
        if (response == null || response.isEmpty()) return failures;

        Object failure = response.get("failure");
        appendFailureItems(failures, failure);

        Object errors = response.get("errors");
        appendFailureItems(failures, errors);

        Object success = response.get("success");
        if (success instanceof Boolean b && !b) {
            Object message = response.get("message");
            if (message != null) failures.add(String.valueOf(message));
        }

        return failures;
    }

    private static void appendFailureItems(List<String> out, Object value) {
        if (value == null) return;
        if (value instanceof List<?> list) {
            for (Object item : list) {
                appendFailureItems(out, item);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            Object error = map.get("error");
            Object listingId = map.get("listing_id");
            Object ratePlanId = map.get("rate_plan_id");

            StringBuilder sb = new StringBuilder();
            if (listingId != null) sb.append("listing_id=").append(listingId).append(' ');
            if (ratePlanId != null) sb.append("rate_plan_id=").append(ratePlanId).append(' ');
            if (error != null) sb.append("error=").append(error);

            String text = sb.length() > 0 ? sb.toString().trim() : map.toString();
            if (!text.isBlank()) out.add(text);
            return;
        }
        String text = String.valueOf(value);
        if (!text.isBlank()) out.add(text);
    }

    static String extractNewToken(Map<String, Object> response) {
        if (response == null || response.isEmpty()) return null;

        Object token = response.get("new_token");
        if (token == null) token = response.get("integration_token");
        if (token == null) token = response.get("token");
        if (token == null) token = response.get("api_key");

        if (token instanceof String s && !s.isBlank()) return s;

        Object data = response.get("data");
        if (data instanceof Map<?, ?> m) {
            Object nested = m.get("new_token");
            if (nested instanceof String s2 && !s2.isBlank()) return s2;
        }

        return null;
    }
}
