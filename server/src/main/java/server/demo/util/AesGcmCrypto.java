package server.demo.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 通用 AES-GCM 凭据加解密工具（随机 IV，Base64 输出）。
 * 密文格式与门锁凭据一致："v1:<base64(nonce)>:<base64(ciphertext+tag)"，
 * 便于后续凭据类存储统一演进；密钥为 32 字节 AES-256，从 Base64 配置读入。
 * 非 Spring 组件：由使用方按配置显式构造，便于"未配置密钥"按功能关闭处理。
 */
public final class AesGcmCrypto {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] keyBytes;

    public AesGcmCrypto(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != KEY_BYTES) {
            throw new IllegalArgumentException("AES-GCM 密钥必须为 32 字节");
        }
        this.keyBytes = keyBytes.clone();
    }

    /** 从 Base64 编码的 32 字节密钥构造；格式非法时抛 IllegalArgumentException。 */
    public static AesGcmCrypto fromBase64Key(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("AES-GCM 密钥不能为空");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("AES-GCM 密钥不是合法的 Base64", ex);
        }
        return new AesGcmCrypto(keyBytes);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce)
            );
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(nonce)
                    + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("凭据加密失败", ex);
        }
    }

    /** 解密失败（密钥不符、密文损坏、格式不支持）一律抛 IllegalStateException，由调用方降级处理。 */
    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            String[] parts = encoded.split(":", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported credential payload");
            }
            byte[] nonce = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce)
            );
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("凭据解密失败", ex);
        }
    }
}
