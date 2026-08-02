package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PushDeviceToken;
import server.demo.i18n.AppLocale;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.util.UtcTimeUtil;

import java.util.Locale;

/**
 * 移动设备推送令牌的注册与解绑。
 */
@Service
public class PushDeviceTokenService {

    private final PushDeviceTokenRepository pushDeviceTokenRepository;

    public PushDeviceTokenService(PushDeviceTokenRepository pushDeviceTokenRepository) {
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
    }

    /**
     * 注册（或刷新）设备令牌。同一设备换账号/换门店登录时更新归属并重新启用。
     * locale 为设备 App 当前语言，缺省/无法识别时回退 zh-CN。
     */
    @Transactional
    public void register(Long userId, Long storeId, server.demo.enums.PushPlatform platform, String deviceToken,
                         String locale) {
        String normalizedToken = deviceToken != null ? deviceToken.trim() : "";
        PushDeviceToken token = pushDeviceTokenRepository.findByDeviceToken(normalizedToken)
                .orElseGet(PushDeviceToken::new);
        token.setUserId(userId);
        token.setStoreId(storeId);
        token.setPlatform(platform);
        token.setDeviceToken(normalizedToken);
        token.setLocale(normalizeLocale(locale));
        token.setEnabled(true);
        token.setLastSeenAt(UtcTimeUtil.nowLocalDateTime());
        pushDeviceTokenRepository.save(token);
    }

    private String normalizeLocale(String locale) {
        Locale normalized = AppLocale.fromTag(locale);
        return normalized != null ? normalized.toLanguageTag() : AppLocale.DEFAULT.toLanguageTag();
    }

    /**
     * 解绑设备令牌（退出登录）。令牌不存在时静默成功，保证重复调用幂等。
     */
    @Transactional
    public void unregister(String deviceToken) {
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            return;
        }
        pushDeviceTokenRepository.deleteByDeviceToken(deviceToken.trim());
    }
}
