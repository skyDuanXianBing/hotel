package server.demo.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SuApiConfig;
import server.demo.exception.SuApiUnauthorizedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Su Channel Manager API 客户端
 * 负责与Su API的所有通信
 */
@Service
public class SuApiClient {

    private static final Logger logger = LoggerFactory.getLogger(SuApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SuApiConfig suApiConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String requireNonBlankTrimmed(String value, String helpMessage) {
        if (value == null) {
            throw new IllegalStateException(helpMessage);
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalStateException(helpMessage);
        }
        return trimmed;
    }

    private String getClientIdOrThrow() {
        return requireNonBlankTrimmed(
                suApiConfig.getClientId(),
                "缺少 Su client-id，请在 application.properties 配置 su.api.client-id 或设置环境变量 SU_CLIENT_ID"
        );
    }

    private String getClientSecretOrThrow() {
        return requireNonBlankTrimmed(
                suApiConfig.getClientSecret(),
                "缺少 Su client-secret，请在 application.properties 配置 su.api.client-secret 或设置环境变量 SU_CLIENT_SECRET"
        );
    }

    private HttpHeaders buildSuAuthHeaders(String accessToken, boolean includeAppId, boolean jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken.trim());
        headers.set("client-id", getClientIdOrThrow());
        headers.set("client-secret", getClientSecretOrThrow());
        if (includeAppId) {
            headers.set("app-id", getClientIdOrThrow());
        }
        if (jsonBody) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }
        return headers;
    }

    private String toSafeHeaderSummary(HttpHeaders headers) {
        if (headers == null) {
            return "{}";
        }
        boolean hasAuth = headers.containsKey("Authorization");
        boolean hasClientId = headers.containsKey("client-id");
        boolean hasClientSecret = headers.containsKey("client-secret");
        boolean hasAppId = headers.containsKey("app-id");
        String contentType = headers.getContentType() != null ? headers.getContentType().toString() : "null";
        return "{Authorization=" + hasAuth
                + ", client-id=" + hasClientId
                + ", client-secret=" + hasClientSecret
                + ", app-id=" + hasAppId
                + ", Content-Type=" + contentType
                + "}";
    }

    /**
     * Su Access Token响应
     */
    public static class SuTokenResponse {
        @JsonAlias({"Status", "status"})
        private String status;  // some endpoints use "Status": "Success"

        @JsonAlias({"success"})
        private Boolean success; // auth endpoint uses { success: true }

        @JsonAlias({"data", "Data"})
        private TokenData data;

        @JsonAlias({"message", "Message"})
        private String message;

        public static class TokenData {
            @JsonProperty("token_type")
            private String tokenType;

            @JsonProperty("token")
            private String token;

            @JsonProperty("expire_in")
            private String expireIn;

            // Getters and Setters
            public String getTokenType() { return tokenType; }
            public void setTokenType(String tokenType) { this.tokenType = tokenType; }
            public String getToken() { return token; }
            public void setToken(String token) { this.token = token; }
            public String getExpireIn() { return expireIn; }
            public void setExpireIn(String expireIn) { this.expireIn = expireIn; }
        }

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public TokenData getData() { return data; }
        public void setData(TokenData data) { this.data = data; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        // Helper method to check if successful
        public boolean isSuccess() {
            if (success != null) {
                return Boolean.TRUE.equals(success);
            }
            return "Success".equalsIgnoreCase(status);
        }
    }

    /**
     * Su Widget Token响应
     */
    public static class SuWidgetTokenResponse {
        @JsonAlias({"Status", "status"})
        private String status;

        @JsonAlias({"success"})
        private Boolean success;

        @JsonAlias({"Data", "data"})
        private WidgetData data;

        @JsonAlias({"Message", "message"})
        private String message;

        public static class WidgetData {
            private String msg;
            @JsonProperty("token_id")
            private String tokenId;

            @JsonProperty("user_name")
            private String userName;

            @JsonProperty("pms_name")
            private String pmsName;
            private String proppmsid;

            // Getters and Setters
            public String getMsg() { return msg; }
            public void setMsg(String msg) { this.msg = msg; }
            public String getTokenId() { return tokenId; }
            public void setTokenId(String tokenId) { this.tokenId = tokenId; }
            public String getUserName() { return userName; }
            public void setUserName(String userName) { this.userName = userName; }
            public String getPmsName() { return pmsName; }
            public void setPmsName(String pmsName) { this.pmsName = pmsName; }
            public String getProppmsid() { return proppmsid; }
            public void setProppmsid(String proppmsid) { this.proppmsid = proppmsid; }
        }

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public WidgetData getData() { return data; }
        public void setData(WidgetData data) { this.data = data; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isSuccess() {
            if (success != null) {
                return Boolean.TRUE.equals(success);
            }
            return "Success".equalsIgnoreCase(status);
        }
    }

    /**
     * 生成Su API Access Token
     * 使用PMS的统一凭证生成token
     */
    public SuTokenResponse generateAccessToken() {
        if (suApiConfig.getClientId() == null || suApiConfig.getClientId().isBlank()) {
            throw new IllegalStateException("缺少 Su client-id，请在 application.properties 配置 su.api.client-id 或设置环境变量 SU_CLIENT_ID");
        }
        if (suApiConfig.getClientSecret() == null || suApiConfig.getClientSecret().isBlank()) {
            throw new IllegalStateException("缺少 Su client-secret，请在 application.properties 配置 su.api.client-secret 或设置环境变量 SU_CLIENT_SECRET");
        }
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/auth/generate-access-token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("client-id", getClientIdOrThrow());
        headers.set("client-secret", getClientSecretOrThrow());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            logger.info("Generating Su API access token");
            logger.debug("Su access token request headers: {}", toSafeHeaderSummary(headers));
            ResponseEntity<SuTokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    SuTokenResponse.class
            );

            SuTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null && tokenResponse.isSuccess()) {
                logger.info("Su API access token generated successfully");
                return tokenResponse;
            } else {
                logger.error("Failed to generate Su API access token: {}",
                        tokenResponse != null ? tokenResponse.getMessage() : "No response");
                throw new RuntimeException("Failed to generate Su API access token: " +
                        (tokenResponse != null ? tokenResponse.getMessage() : "No response"));
            }
        } catch (Exception e) {
            logger.error("Error generating Su API access token", e);
            throw new RuntimeException("Error generating Su API access token: " + e.getMessage(), e);
        }
    }

    /**
     * 生成Widget Token
     * 用于加载Su Channel Mapping Widget
     *
     * @param hotelId  物业ID (通常使用 "store_{storeId}" 格式)
     * @param accessToken Su API access token
     * @return Widget token响应
     */
    public SuWidgetTokenResponse getWidgetToken(String hotelId, String accessToken) {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId 不能为空");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken 不能为空");
        }
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/widget/getWidgetAccessToken";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("hotelid", hotelId);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("Generating Su Widget token for hotel: {}", hotelId);
            logger.debug("Su widget token request url={}, headers={}", url, toSafeHeaderSummary(headers));
            ResponseEntity<SuWidgetTokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    SuWidgetTokenResponse.class
            );

            SuWidgetTokenResponse widgetResponse = response.getBody();
            if (widgetResponse != null && widgetResponse.isSuccess()) {
                logger.info("Su Widget token generated successfully for hotel: {}", hotelId);
                return widgetResponse;
            } else {
                logger.error("Failed to generate Su Widget token: {}",
                        widgetResponse != null ? widgetResponse.getMessage() : "No response");
                throw new RuntimeException("Failed to generate Su Widget token: " +
                        (widgetResponse != null ? widgetResponse.getMessage() : "No response"));
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            // 兜底：部分 Su 沙盒账号对该接口的 header 要求可能不同，尝试带 app-id 再请求一次。
            logger.warn("Su widget token unauthorized (401). Retrying with app-id header. hotelId={}", hotelId);
            try {
                HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
                HttpEntity<Map<String, String>> retryRequest = new HttpEntity<>(requestBody, retryHeaders);
                logger.debug("Su widget token retry url={}, headers={}", url, toSafeHeaderSummary(retryHeaders));

                ResponseEntity<SuWidgetTokenResponse> retryResponse = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        retryRequest,
                        SuWidgetTokenResponse.class
                );

                SuWidgetTokenResponse widgetResponse = retryResponse.getBody();
                if (widgetResponse != null && widgetResponse.isSuccess()) {
                    logger.info("Su Widget token generated successfully (after retry) for hotel: {}", hotelId);
                    return widgetResponse;
                }
                throw new RuntimeException("Failed to generate Su Widget token after retry: " +
                        (widgetResponse != null ? widgetResponse.getMessage() : "No response"));
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su widget token retry returned client error. status={}, body={}", retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException("getWidgetToken", "Su widget token unauthorized (401) after retry", retryErr);
                }
                throw new RuntimeException("Error generating Su Widget token after retry: " + retryErr.getStatusCode(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su widget token returned client error. status={}, body={}", e.getStatusCode(), body);
            if (e instanceof HttpClientErrorException.Unauthorized) {
                throw new SuApiUnauthorizedException("getWidgetToken", "Su widget token unauthorized (401)", e);
            }
            throw new RuntimeException("Error generating Su Widget token: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error generating Su Widget token for hotel: {}", hotelId, e);
            throw new RuntimeException("Error generating Su Widget token: " + e.getMessage(), e);
        }
    }

    /**
     * 拉取预订（Reservation）
     * 文档：Su Channel Manager -> Reservations -> Request for Reservation Information
     * Endpoint: POST /SUAPI/jservice/Reservation
     *
     * @param accessToken Su API access token
     * @param hotelId     IT Provider hotel id（本项目使用 stores.su_hotel_id / 默认 STORE{storeId}）
     */
    /**
     * PMS 房型同步（供 Su Widget 映射下拉使用）
     * Endpoint: POST /SUAPI/jservice/OTA_HotelRoom
     */
    public JsonNode postOtaHotelRoom(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/OTA_HotelRoom";
        return postSuJsonWithAuthRetry(url, accessToken, payload, "OTA_HotelRoom");
    }

    /**
     * PMS 费率计划同步（供 Su Widget 映射下拉使用）
     * Endpoint: POST /SUAPI/jservice/OTA_HotelRatePlan
     */
    public JsonNode postOtaHotelRatePlan(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/OTA_HotelRatePlan";
        return postSuJsonWithAuthRetry(url, accessToken, payload, "OTA_HotelRatePlan");
    }

    /**
     * 查询当前已映射的渠道/房型/费率（用于校验 Widget 保存是否生效）
     * Endpoint: POST /SUAPI/jservice/mappings
     */
    public JsonNode getMappings(String accessToken, String hotelId, String channelId) {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId 不能为空");
        }
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/mappings";

        Map<String, Object> payload = new HashMap<>();
        payload.put("hotelid", hotelId);
        if (channelId != null && !channelId.isBlank()) {
            payload.put("channelid", channelId);
        }
        return postSuJsonWithAuthRetry(url, accessToken, payload, "mappings");
    }

    private JsonNode postSuJsonWithAuthRetry(String url, String accessToken, Object payload, String actionName) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken 不能为空");
        }
        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);
        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Posting Su {} request", actionName);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su " + actionName + " returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Su {} unauthorized (401). Retrying with app-id header.", actionName);
            HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
            HttpEntity<Object> retryRequest = new HttpEntity<>(payload, retryHeaders);
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, retryRequest, String.class);
                String body = response.getBody();
                if (body == null || body.isBlank()) {
                    throw new RuntimeException("Su " + actionName + " returned empty response body (after retry)");
                }
                return objectMapper.readTree(body);
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su {} retry returned client error. status={}, body={}", actionName, retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException(actionName, "Su " + actionName + " unauthorized (401) after retry", retryErr);
                }
                if (body != null && !body.isBlank()) {
                    try {
                        return objectMapper.readTree(body);
                    } catch (Exception ignore) {
                        // fallthrough
                    }
                }
                throw new RuntimeException("Su " + actionName + " returned error after retry: " + retryErr.getStatusCode(), retryErr);
            } catch (Exception retryErr) {
                logger.error("Error calling Su {} (after retry)", actionName, retryErr);
                throw new RuntimeException("Error calling Su " + actionName + " after retry: " + retryErr.getMessage(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su {} returned client error. status={}, body={}", actionName, e.getStatusCode(), body);
            if (e instanceof HttpClientErrorException.Unauthorized) {
                throw new SuApiUnauthorizedException(actionName, "Su " + actionName + " unauthorized (401)", e);
            }
            if (body != null && !body.isBlank()) {
                try {
                    return objectMapper.readTree(body);
                } catch (Exception ignore) {
                    // fallthrough
                }
            }
            throw new RuntimeException("Su " + actionName + " returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su {}", actionName, e);
            throw new RuntimeException("Error calling Su " + actionName + ": " + e.getMessage(), e);
        }
    }

    public JsonNode pullReservations(String accessToken, String hotelId) {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId 不能为空");
        }
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/Reservation";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("hotelid", hotelId);
        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Pulling Su reservations. hotelId={}", hotelId);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su Reservation returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Su Reservation unauthorized (401). Retrying with app-id header. hotelId={}", hotelId);
            try {
                HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
                HttpEntity<Object> retryRequest = new HttpEntity<>(payload, retryHeaders);
                ResponseEntity<String> retryResponse = restTemplate.exchange(url, HttpMethod.POST, retryRequest, String.class);
                String body = retryResponse.getBody();
                if (body == null || body.isBlank()) {
                    throw new RuntimeException("Su Reservation returned empty response body (after retry)");
                }
                return objectMapper.readTree(body);
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su Reservation retry returned client error. status={}, body={}", retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException("Reservation", "Su Reservation unauthorized (401) after retry", retryErr);
                }
                throw new RuntimeException("Su Reservation returned error after retry: " + retryErr.getStatusCode(), retryErr);
            } catch (Exception retryErr) {
                logger.error("Error calling Su Reservation (after retry)", retryErr);
                throw new RuntimeException("Error calling Su Reservation after retry: " + retryErr.getMessage(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su Reservation returned client error. status={}, body={}", e.getStatusCode(), body);
            throw new RuntimeException("Su Reservation returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su Reservation", e);
            throw new RuntimeException("Error calling Su Reservation: " + e.getMessage(), e);
        }
    }

    /**
     * 确认已成功接收预订通知（Reservation_notif）
     * 文档：Su Channel Manager -> Reservations -> Reservation Notification
     * Endpoint: POST /SUAPI/jservice/Reservation_notif
     *
     * @param accessToken Su API access token
     * @param hotelId     IT Provider hotel id
     * @param notifIds    reservation_notif_id 列表
     */
    public JsonNode ackReservationNotifs(String accessToken, String hotelId, List<String> notifIds) {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId 不能为空");
        }
        if (notifIds == null || notifIds.isEmpty()) {
            throw new IllegalArgumentException("notifIds 不能为空");
        }

        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/Reservation_notif";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("reservation_notif", Map.of("reservation_notif_id", notifIds));

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Ack Su reservation notifications. hotelId={}, count={}", hotelId, notifIds.size());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su Reservation_notif returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Su Reservation_notif unauthorized (401). Retrying with app-id header. hotelId={}", hotelId);
            try {
                HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
                HttpEntity<Object> retryRequest = new HttpEntity<>(payload, retryHeaders);
                ResponseEntity<String> retryResponse = restTemplate.exchange(url, HttpMethod.POST, retryRequest, String.class);
                String body = retryResponse.getBody();
                if (body == null || body.isBlank()) {
                    throw new RuntimeException("Su Reservation_notif returned empty response body (after retry)");
                }
                return objectMapper.readTree(body);
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su Reservation_notif retry returned client error. status={}, body={}", retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException("Reservation_notif", "Su Reservation_notif unauthorized (401) after retry", retryErr);
                }
                throw new RuntimeException("Su Reservation_notif returned error after retry: " + retryErr.getStatusCode(), retryErr);
            } catch (Exception retryErr) {
                logger.error("Error calling Su Reservation_notif (after retry)", retryErr);
                throw new RuntimeException("Error calling Su Reservation_notif after retry: " + retryErr.getMessage(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su Reservation_notif returned client error. status={}, body={}", e.getStatusCode(), body);
            throw new RuntimeException("Su Reservation_notif returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su Reservation_notif", e);
            throw new RuntimeException("Error calling Su Reservation_notif: " + e.getMessage(), e);
        }
    }

    /**
     * 验证Access Token是否有效
     *
     * @param accessToken Su API access token
     * @return true if valid, false otherwise
     */
    /**
     * Request Booking - Confirm/Accept
     * 文档：docs/预订/确认预订请求.txt
     * Endpoint: POST /SUAPI/service/requestbookings
     */
    public JsonNode confirmRequestBooking(String accessToken, String bookingId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId 不能为空");
        }

        String url = suApiConfig.getBaseUrl() + "/SUAPI/service/requestbookings";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingid", bookingId.trim());

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Confirm request booking. bookingId={}", bookingId);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su requestbookings returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Su requestbookings(confirm) unauthorized (401). Retrying with app-id header. bookingId={}", bookingId);
            try {
                HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
                HttpEntity<Object> retryRequest = new HttpEntity<>(payload, retryHeaders);
                ResponseEntity<String> retryResponse = restTemplate.exchange(url, HttpMethod.POST, retryRequest, String.class);
                String body = retryResponse.getBody();
                if (body == null || body.isBlank()) {
                    throw new RuntimeException("Su requestbookings returned empty response body (after retry)");
                }
                return objectMapper.readTree(body);
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su requestbookings(confirm) retry returned client error. status={}, body={}", retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException("requestbookings(confirm)", "Su requestbookings(confirm) unauthorized (401) after retry", retryErr);
                }
                throw new RuntimeException("Su requestbookings(confirm) returned error after retry: " + retryErr.getStatusCode(), retryErr);
            } catch (Exception retryErr) {
                logger.error("Error calling Su requestbookings(confirm) (after retry)", retryErr);
                throw new RuntimeException("Error calling Su requestbookings(confirm) after retry: " + retryErr.getMessage(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su requestbookings(confirm) returned client error. status={}, body={}", e.getStatusCode(), body);
            throw new RuntimeException("Su requestbookings(confirm) returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su requestbookings(confirm)", e);
            throw new RuntimeException("Error calling Su requestbookings(confirm): " + e.getMessage(), e);
        }
    }

    /**
     * Request Booking - Deny/Cancel (status=cancel)
     * 文档：docs/预订/取消预订请求.txt
     * Endpoint: POST /SUAPI/service/requestbookings
     */
    public JsonNode denyRequestBooking(
            String accessToken,
            String bookingId,
            String declineReason,
            String messageGuest,
            String messageAirbnb
    ) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId 不能为空");
        }

        String url = suApiConfig.getBaseUrl() + "/SUAPI/service/requestbookings";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingid", bookingId.trim());
        payload.put("status", "cancel");

        if (declineReason != null && !declineReason.isBlank()) {
            payload.put("decline_reason", declineReason.trim());
        }
        if (messageGuest != null && !messageGuest.isBlank()) {
            payload.put("message_guest", messageGuest.trim());
        }
        if (messageAirbnb != null && !messageAirbnb.isBlank()) {
            payload.put("message_airbnb", messageAirbnb.trim());
        }

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Deny request booking. bookingId={}", bookingId);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su requestbookings returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Su requestbookings(deny) unauthorized (401). Retrying with app-id header. bookingId={}", bookingId);
            try {
                HttpHeaders retryHeaders = buildSuAuthHeaders(accessToken, true, true);
                HttpEntity<Object> retryRequest = new HttpEntity<>(payload, retryHeaders);
                ResponseEntity<String> retryResponse = restTemplate.exchange(url, HttpMethod.POST, retryRequest, String.class);
                String body = retryResponse.getBody();
                if (body == null || body.isBlank()) {
                    throw new RuntimeException("Su requestbookings returned empty response body (after retry)");
                }
                return objectMapper.readTree(body);
            } catch (HttpClientErrorException retryErr) {
                String body = retryErr.getResponseBodyAsString();
                logger.error("Su requestbookings(deny) retry returned client error. status={}, body={}", retryErr.getStatusCode(), body);
                if (retryErr instanceof HttpClientErrorException.Unauthorized) {
                    throw new SuApiUnauthorizedException("requestbookings(deny)", "Su requestbookings(deny) unauthorized (401) after retry", retryErr);
                }
                throw new RuntimeException("Su requestbookings(deny) returned error after retry: " + retryErr.getStatusCode(), retryErr);
            } catch (Exception retryErr) {
                logger.error("Error calling Su requestbookings(deny) (after retry)", retryErr);
                throw new RuntimeException("Error calling Su requestbookings(deny) after retry: " + retryErr.getMessage(), retryErr);
            }
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su requestbookings(deny) returned client error. status={}, body={}", e.getStatusCode(), body);
            throw new RuntimeException("Su requestbookings(deny) returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su requestbookings(deny)", e);
            throw new RuntimeException("Error calling Su requestbookings(deny): " + e.getMessage(), e);
        }
    }

    public boolean validateAccessToken(String accessToken) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/pmsproperty";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, false, false);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return true;
        } catch (Exception e) {
            logger.warn("Su API access token validation failed", e);
            return false;
        }
    }

    /**
     * 获取渠道代码映射
     * 根据OTA代码获取Su的加密渠道代码
     */
    public String getEncryptedChannelCode(String otaCode) {
        Map<String, String> channelCodeMap = Map.of(
                "AIRBNB", "aM4JjiWOnUx5qS2IT8wHCbVmIWbA9tTD3PFcjnt8M-Y",
                "BOOKING", "Qa9Qwq4PF32srUVea3mYzzvBFiszeXK4aaQINYhXlm8",
                "EXPEDIA", "_4PYESNQm9vU15C3DR4xRrW2VHVrEVGPdhx4du8_uBw",
                "AGODA", "sAr2QsPWYcMUS-7PKJtEDGG0aZODNK5Sv4B5o2LTPA0",
                "CTRIP", "mvYVz5x5ExxioyfyMo3jUUpNVZVbMyC6SUExMG9iaIY",
                "VRBO", "6w9fCl2fQYkSXlG4pJXMFegJVyWDk7K0IHzqmjm2egI"
        );
        return channelCodeMap.getOrDefault(otaCode.toUpperCase(), "");
    }

    /**
     * Su Property API - Create / Update Property (OTA_HotelDescriptiveContentNotif)
     * 文档：Su Channel Manager -> Content -> Property
     */
    public JsonNode upsertProperty(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/OTA_HotelDescriptiveContentNotif";

        HttpHeaders headers = buildSuAuthHeaders(accessToken, true, true);

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            logger.info("Posting Su OTA_HotelDescriptiveContentNotif (property upsert)");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("Su property upsert returned empty response body");
            }
            return objectMapper.readTree(body);
        } catch (HttpClientErrorException.Unauthorized e) {
            String body = e.getResponseBodyAsString();
            logger.error("Su property upsert unauthorized (401). body={}", body);
            throw new SuApiUnauthorizedException("OTA_HotelDescriptiveContentNotif", "Su property upsert unauthorized (401)", e);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    return objectMapper.readTree(body);
                } catch (Exception parseError) {
                    // fallthrough
                }
            }
            logger.error("Su property upsert returned client error. status={}, body={}", e.getStatusCode(), body);
            throw new RuntimeException("Su property upsert returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error calling Su property upsert", e);
            throw new RuntimeException("Error calling Su property upsert: " + e.getMessage(), e);
        }
    }

    /**
     * 判断 Su JSON 响应是否成功（兼容 Status/Success/status 等字段）
     */
    public boolean isSuSuccess(JsonNode response) {
        if (response == null) {
            return true;
        }
        JsonNode successNode = response.get("Success");
        if (successNode != null) {
            String success = successNode.asText("");
            return "Success".equalsIgnoreCase(success) || "SUCCESS".equalsIgnoreCase(success);
        }
        JsonNode statusNode = response.get("status");
        if (statusNode == null) {
            statusNode = response.get("Status");
        }
        if (statusNode == null) {
            return true;
        }
        String status = statusNode.asText("");
        return "Success".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * 尝试从 Su 错误响应提取可读的错误信息（Errors.ShortText / message / Message）
     */
    public String extractSuErrorMessage(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode messageNode = response.get("message");
        if (messageNode == null) {
            messageNode = response.get("Message");
        }
        if (messageNode != null && !messageNode.asText("").isBlank()) {
            return messageNode.asText("");
        }

        JsonNode errorsNode = response.get("Errors");
        if (errorsNode == null) {
            errorsNode = response.get("errors");
        }
        if (errorsNode == null) {
            return null;
        }

        if (errorsNode.isArray() && errorsNode.size() > 0) {
            JsonNode first = errorsNode.get(0);
            JsonNode shortText = first != null ? first.get("ShortText") : null;
            if (shortText == null && first != null) {
                shortText = first.get("shortText");
            }
            if (shortText != null && !shortText.asText("").isBlank()) {
                return shortText.asText("");
            }
        }

        if (errorsNode.isObject()) {
            JsonNode shortText = errorsNode.get("ShortText");
            if (shortText == null) {
                shortText = errorsNode.get("shortText");
            }
            if (shortText != null && !shortText.asText("").isBlank()) {
                return shortText.asText("");
            }
        }

        return errorsNode.toString();
    }

    /**
     * Su ARI Control API - Inventory Control
     * https://suissu.gitbook.io/su-api-documentation/su-channel-manager/inventory-control
     */
    public JsonNode postInvRateControl(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/invratecontrol";
        try {
            return postSuJsonWithAuthRetry(url, accessToken, payload, "invratecontrol");
        } catch (Exception e) {
            logger.error("Error calling Su invratecontrol", e);
            throw new RuntimeException("Error calling Su invratecontrol: " + e.getMessage(), e);
        }
    }

    /**
     * Rates and Availability API (base ARI)
     * https://suissu.gitbook.io/su-api-documentation/su-channel-manager/rates-and-availability
     */
    public JsonNode postAvailability(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/availability";
        try {
            return postSuJsonWithAuthRetry(url, accessToken, payload, "availability");
        } catch (Exception e) {
            logger.error("Error calling Su availability", e);
            throw new RuntimeException("Error calling Su availability: " + e.getMessage(), e);
        }
    }

    /**
     * Su Messaging Reply API
     * 文档：docs/消息传递.txt
     * Endpoint: POST /SUAPI/jservice/messagingAB
     */
    public JsonNode postMessagingAB(String accessToken, Object payload) {
        String url = suApiConfig.getBaseUrl() + "/SUAPI/jservice/messagingAB";
        try {
            return postSuJsonWithAuthRetry(url, accessToken, payload, "messagingAB");
        } catch (Exception e) {
            logger.error("Error calling Su messagingAB", e);
            throw new RuntimeException("Error calling Su messagingAB: " + e.getMessage(), e);
        }
    }
}
