package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PushDeviceToken;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.util.UtcTimeUtil;

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
     */
    @Transactional
    public void register(Long userId, Long storeId, server.demo.enums.PushPlatform platform, String deviceToken) {
        String normalizedToken = deviceToken != null ? deviceToken.trim() : "";
        PushDeviceToken token = pushDeviceTokenRepository.findByDeviceToken(normalizedToken)
                .orElseGet(PushDeviceToken::new);
        token.setUserId(userId);
        token.setStoreId(storeId);
        token.setPlatform(platform);
        token.setDeviceToken(normalizedToken);
        token.setEnabled(true);
        token.setLastSeenAt(UtcTimeUtil.nowLocalDateTime());
        pushDeviceTokenRepository.save(token);
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
