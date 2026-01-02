package server.demo.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PriceLabs Webhook 签名验证工具
 */
public class PriceLabsSignatureVerifier {

    private final String integrationToken;

    public PriceLabsSignatureVerifier(String integrationToken) {
        this.integrationToken = integrationToken;
    }

    /**
     * 验证 PriceLabs Webhook 签名
     *
     * @param requestBody 请求体原始字符串
     * @param signature X-Signature 请求头的值
     * @return 签名是否有效
     */
    public boolean verifySignature(String requestBody, String signature) {
        if (requestBody == null || signature == null) {
            return false;
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                integrationToken.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            System.err.println("签名验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 生成签名（用于测试）
     *
     * @param requestBody 请求体字符串
     * @return Base64 编码的签名
     */
    public String generateSignature(String requestBody) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                integrationToken.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败: " + e.getMessage(), e);
        }
    }
}
