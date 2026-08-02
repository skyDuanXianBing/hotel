package server.demo.service.push;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.config.PushProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * iOS APNs 推送发送器（Token 认证，.p8 Auth Key）。
 *
 * <p>未配置密钥或开关关闭时进入 disabled 状态：所有发送请求返回
 * {@link ApnsSendResult.Status#DISABLED}，不影响业务主流程。
 */
@Service
public class ApnsPushService {

    private static final Logger logger = LoggerFactory.getLogger(ApnsPushService.class);

    private final PushProperties pushProperties;

    private volatile ApnsClient apnsClient;

    public ApnsPushService(PushProperties pushProperties) {
        this.pushProperties = pushProperties;
    }

    @PostConstruct
    void init() {
        PushProperties.Apns apns = pushProperties.getApns();
        if (!pushProperties.isEnabled() || !apns.isEnabled()) {
            logger.info("APNs push disabled (push.enabled={}, push.apns.enabled={})",
                    pushProperties.isEnabled(), apns.isEnabled());
            return;
        }

        try {
            ApnsSigningKey signingKey = loadSigningKey(apns);
            if (signingKey == null) {
                logger.warn("APNs push disabled: no auth key configured (path/base64 both empty)");
                return;
            }

            String host = apns.isProduction()
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
            apnsClient = new ApnsClientBuilder()
                    .setApnsServer(host)
                    .setSigningKey(signingKey)
                    .build();
            logger.info("APNs push client initialized. host={}, topic={}", host, apns.getTopic());
        } catch (Exception e) {
            logger.error("APNs push client init failed, push stays disabled: {}", e.getMessage(), e);
            apnsClient = null;
        }
    }

    @PreDestroy
    void shutdown() {
        ApnsClient client = apnsClient;
        apnsClient = null;
        if (client != null) {
            try {
                client.close().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.warn("APNs client close failed: {}", e.getMessage());
            }
        }
    }

    public boolean isReady() {
        return apnsClient != null;
    }

    /**
     * 发送一条带标题/正文/声音的提醒推送；customData 会放入 payload 顶层供 App 点击路由。
     */
    public CompletableFuture<ApnsSendResult> send(String deviceToken, String title, String body,
                                                  Map<String, String> customData) {
        ApnsClient client = apnsClient;
        if (client == null) {
            return CompletableFuture.completedFuture(ApnsSendResult.disabled(deviceToken));
        }

        SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");
        if (customData != null) {
            customData.forEach(payloadBuilder::addCustomProperty);
        }

        SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                deviceToken, pushProperties.getApns().getTopic(), payloadBuilder.build());

        CompletableFuture<ApnsSendResult> result = new CompletableFuture<>();
        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> future =
                client.sendNotification(pushNotification);
        future.whenComplete((response, cause) -> {
            if (cause != null) {
                logger.warn("APNs send failed. token={}..., err={}", abbreviate(deviceToken), cause.getMessage());
                result.complete(ApnsSendResult.rejected(deviceToken, "IO_ERROR:" + cause.getMessage()));
                return;
            }
            if (response.isAccepted()) {
                result.complete(ApnsSendResult.accepted(deviceToken));
            } else {
                String reason = response.getRejectionReason().orElse("UNKNOWN");
                logger.warn("APNs rejected. token={}..., reason={}", abbreviate(deviceToken), reason);
                result.complete(ApnsSendResult.rejected(deviceToken, reason));
            }
        });
        return result;
    }

    private ApnsSigningKey loadSigningKey(PushProperties.Apns apns) throws Exception {
        String teamId = apns.getTeamId();
        String keyId = apns.getKeyId();
        if (isBlank(teamId) || isBlank(keyId)) {
            logger.warn("APNs push disabled: teamId/keyId not configured");
            return null;
        }

        if (!isBlank(apns.getAuthKeyPath())) {
            return ApnsSigningKey.loadFromPkcs8File(new File(apns.getAuthKeyPath().trim()), teamId, keyId);
        }
        if (!isBlank(apns.getAuthKeyBase64())) {
            byte[] keyBytes = Base64.getDecoder().decode(apns.getAuthKeyBase64().trim());
            try (InputStream in = new ByteArrayInputStream(keyBytes)) {
                return ApnsSigningKey.loadFromInputStream(in, teamId, keyId);
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String abbreviate(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8);
    }

    /**
     * 单次推送结果；rejectionReason 为 APNs 返回的拒绝原因（BadDeviceToken/Unregistered 等）。
     */
    public record ApnsSendResult(Status status, String deviceToken, String rejectionReason) {

        public enum Status {
            ACCEPTED,
            REJECTED,
            DISABLED
        }

        static ApnsSendResult accepted(String deviceToken) {
            return new ApnsSendResult(Status.ACCEPTED, deviceToken, null);
        }

        static ApnsSendResult rejected(String deviceToken, String rejectionReason) {
            return new ApnsSendResult(Status.REJECTED, deviceToken, rejectionReason);
        }

        static ApnsSendResult disabled(String deviceToken) {
            return new ApnsSendResult(Status.DISABLED, deviceToken, null);
        }
    }
}
